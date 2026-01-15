package com.make.quartz.service;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobExecutionLog;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.mapper.SysJobExecutionLogMapper;
import com.make.quartz.mapper.SysJobRuntimeMapper;
import com.make.quartz.util.JobInvokeUtil;
import com.make.common.core.NodeRegistry;
import com.make.quartz.util.RedisMessageQueue;
import com.make.quartz.util.TaskDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 队列消费执行服务（Redis-only）
 *
 * <p>职责：
 * 1) 启动当前节点的队列监听（每节点消费自己的队列）
 * 2) 收到消息后执行 SysJob（invokeTarget）
 * 3) 执行成功后：计算下一次触发时间，并重新入队（实现“定时任务”的循环）
 *
 * <p>注意：
 * - Scheduled 时间检查已经在 RedisMessageQueue 内通过 delay zset 实现
 * - 这里仅负责“执行”和“续约下一次”
 */
@Component
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    @Autowired
    private SysJobRuntimeMapper sysJobRuntimeMapper;

    @Autowired
    private SysJobExecutionLogMapper sysJobExecutionLogMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TaskDistributor taskDistributor;

    private static final String DEDUP_KEY_PREFIX = "mq:job:dedup:";
    private static final String RUNTIME_CACHE_PREFIX = "mq:job:runtime:";
    private static final String TASK_MONITOR_PREFIX = "TASK_MONITOR:"; // Requirement 3

    /**
     * 防止同 executionId 重入
     */
    private final ConcurrentHashMap<String, Long> executing = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String nodeId = NodeRegistry.getCurrentNodeId();
        RedisMessageQueue.getInstance().startListening(nodeId, this::handle);
    }


    /**
     * Requirement 3: 服务停止时释放占用任务
     */
    @PreDestroy
    public void destroy() {
        log.info("[SHUTDOWN] Starting TaskExecutionService shutdown...");
        RedisMessageQueue.getInstance().stopListening();

        try {
            String currentNodeId = NodeRegistry.getCurrentNodeId();

            // 1. 查询当前节点正在执行的任务 (status='RUNNING' and node_id=current)
            SysJobRuntime query = new SysJobRuntime();
            query.setNodeId(currentNodeId);
            query.setStatus("RUNNING");
            List<SysJobRuntime> runningTasks = sysJobRuntimeMapper.selectSysJobRuntimeList(query);

            if (runningTasks != null && !runningTasks.isEmpty()) {
                log.info("[SHUTDOWN_RELEASE] Found {} running tasks on node={}", runningTasks.size(), currentNodeId);

                for (SysJobRuntime task : runningTasks) {
                    try {
                        String execId = task.getExecutionId();
                        Long jobId = task.getJobId();

                        // 2. 清理 Redis 锁 (Dedup & Runtime & Monitor)
                        redisTemplate.delete(DEDUP_KEY_PREFIX + jobId);
                        redisTemplate.delete(RUNTIME_CACHE_PREFIX + execId);
                        redisTemplate.delete(TASK_MONITOR_PREFIX + jobId); // Clean monitor key

                        // 3. 更新 DB 状态回退为 WAITING (让其他节点可恢复)，并清除 node_id
                        task.setStatus("WAITING");
                        task.setNodeId(null); // Clear ownership
                        // Mybatis update selective 会忽略 null，所以我们需要显式设置为 "" 或者确保 SQL 支持 null
                        // 查看 Mapper XML，if test="nodeId != null" 才更新。
                        // 这里我们可能需要一种方式置空 node_id，或者只改状态让 node_id 失效。
                        // 只要 status=WAITING，TaskRecoveryService 就会认为它是待处理的，node_id 并不影响 active 判定 (countRunningOrWaiting 不看 node_id)
                        // 但为了干净，最好能置空。Mapper 不支持 update null。
                        // 暂时只改状态。
                        sysJobRuntimeMapper.updateSysJobRuntime(task);

                        log.info("TASK_LIFECYCLE|RELEASE|jobId={}|executionId={}|nodeId={}|msg=Shutdown release", jobId, execId, currentNodeId);
                    } catch (Exception e) {
                        log.error("[SHUTDOWN_RELEASE_ERR] executionId={} err={}", task.getExecutionId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[SHUTDOWN_ERR] Error during task release", e);
        }

        log.info("[SHUTDOWN] TaskExecutionService shutdown complete.");
    }

    /**
     * 处理一条队列消息
     */
    private void handle(RedisMessageQueue.TaskMessage message) throws Exception {
        // 1. 获取 executionId (优先使用 message 中的, 其次 traceId)
        String execId = message.getExecutionId();
        if (StringUtils.isEmpty(execId)) {
            execId = message.getTraceId();
        }
        if (StringUtils.isEmpty(execId)) {
            // 如果依然没有，说明是旧消息或异常消息，生成一个新的
            execId = UUID.randomUUID().toString();
        }

        MDC.put("traceId", execId);

        try {
            // 2. 幂等检查 (DB 乐观锁抢占)
            // UPDATE sys_job_runtime SET status='RUNNING' ... WHERE execution_id=? AND status='WAITING'
            String nodeId = NodeRegistry.getCurrentNodeId();
            int rows = sysJobRuntimeMapper.updateStatusToRunning(execId, nodeId);

            if (rows == 0) {
                // 抢占失败：可能是重复消费，或者任务已经被其他节点抢占，或者状态不是 WAITING
                log.info("[EXEC_IDEMPOTENT] Update status failed (rows=0). executionId={}, nodeId={}", execId, nodeId);
                // 视为重复投递，直接 ACK (返回即 ACK)
                return;
            }
            log.info("[RUNTIME_RUNNING] jobId={} executionId={} nodeId={}", message.getTaskId(), execId, nodeId);

            // 3. 执行任务
            SysJob sysJob = (SysJob) message.getJobData();
            Date scheduledTime = null;

            // 如果 message 中只有 ID (符合新规范)，则需要回查 SysJobRuntime
            if (sysJob == null) {
                // 尝试从 Runtime Cache 或 DB 获取
                 String runtimeJson = redisTemplate.opsForValue().get(RUNTIME_CACHE_PREFIX + execId);
                 if (StringUtils.isNotEmpty(runtimeJson)) {
                     SysJobRuntime runtime = JSON.parseObject(runtimeJson, SysJobRuntime.class);
                     sysJob = JSON.parseObject(runtime.getPayload(), SysJob.class);
                     scheduledTime = runtime.getScheduledTime();
                 } else {
                     SysJobRuntime runtime = sysJobRuntimeMapper.selectSysJobRuntimeByExecutionId(execId);
                     if (runtime != null) {
                         sysJob = JSON.parseObject(runtime.getPayload(), SysJob.class);
                         scheduledTime = runtime.getScheduledTime();
                     }
                 }
            }

            // Fallback for scheduledTime
            if (scheduledTime == null) {
                // Try to infer from message score if available, or just use now
                scheduledTime = new Date();
            }

            if (sysJob == null || StringUtils.isEmpty(sysJob.getInvokeTarget())) {
                log.warn("[EXEC_DROP] invalid message/payload missing, executionId={}", execId);
                // 无法执行，标记失败并清理
                handleFinalCleanup(execId, null, "FAILED", "Missing payload", 0L, scheduledTime);
                return;
            }

            // Requirement 2: 消费任务日志
            log.info("TASK_LIFECYCLE|CONSUME|jobId={}|executionId={}|nodeId={}|target={}",
                    sysJob.getJobId(), execId, nodeId, sysJob.getInvokeTarget());

            long startTime = System.currentTimeMillis();
            String status = "SUCCESS";
            String errorMsg = null;

            try {
                // log.info("[EXEC_START] jobId={} executionId={} target={}", sysJob.getJobId(), execId, sysJob.getInvokeTarget());
                executeOnce(sysJob);
            } catch (Exception e) {
                status = "FAILED";
                errorMsg = StringUtils.substring(e.getMessage(), 0, 500);
                log.error("[EXEC_FAIL] executionId={}", execId, e);
                // Requirement 2: 异常日志
                log.info("TASK_LIFECYCLE|FAIL|jobId={}|executionId={}|nodeId={}|costMs={}|errorMsg={}",
                        sysJob.getJobId(), execId, nodeId, (System.currentTimeMillis() - startTime), errorMsg);

                // Detailed failure logging for replay/debug
                log.error("[TASK_FAIL_DETAIL] |jobId={}|executionId={}|nodeId={}|traceId={}|target={}|errorType={}|errorMsg={}|stackTrace={}",
                        sysJob.getJobId(), execId, nodeId, MDC.get("traceId"), sysJob.getInvokeTarget(),
                        e.getClass().getSimpleName(), e.getMessage(), org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                // log.info("[EXEC_END] jobId={} executionId={} status={} durationMs={}", sysJob.getJobId(), executionId, status, duration);

                if ("SUCCESS".equals(status)) {
                    // Requirement 2: 完成日志
                    log.info("TASK_LIFECYCLE|COMPLETE|jobId={}|executionId={}|nodeId={}|costMs={}",
                            sysJob.getJobId(), execId, nodeId, duration);
                }

                // 4. 结束尽力而为清理 (Insert Log -> Delete Runtime -> Delete Redis)
                handleFinalCleanup(execId, sysJob, status, errorMsg, duration, scheduledTime);
            }

            // 5. 循环任务续约 (仅当执行成功且配置了 cron)
            if ("SUCCESS".equals(status)) {
                scheduleNextIfNeeded(sysJob, message.getTargetNode(), message.getPriority());
            }

        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 最终清理逻辑
     */
    private void handleFinalCleanup(String executionId, SysJob sysJob, String status, String errorMsg, Long duration, Date scheduledTime) {
        try {
            // 1. 插入 sys_job_execution_log
            SysJobExecutionLog logEntry = new SysJobExecutionLog();
            logEntry.setExecutionId(executionId);
            if (sysJob != null) {
                logEntry.setJobId(sysJob.getJobId());
                logEntry.setJobName(sysJob.getJobName());
                logEntry.setJobGroup(sysJob.getJobGroup());
                logEntry.setPayload(JSON.toJSONString(sysJob));
            }
            logEntry.setStatus(status);
            logEntry.setNodeId(NodeRegistry.getCurrentNodeId());
            logEntry.setStartTime(new Date(System.currentTimeMillis() - duration));
            logEntry.setEndTime(new Date());
            logEntry.setDurationMs(duration);
            logEntry.setErrorMessage(errorMsg);
            // Fix: ensure scheduledTime is set
            logEntry.setScheduledTime(scheduledTime != null ? scheduledTime : logEntry.getStartTime());

            int rows = sysJobExecutionLogMapper.insertSysJobExecutionLog(logEntry);
            log.info("[EXEC_LOG_INSERT] jobId={} executionId={} status={} rows={}", sysJob != null ? sysJob.getJobId() : "?", executionId, status, rows);

        } catch (Exception e) {
            log.error("[EXEC_CLEANUP_LOG_FAIL] executionId={}", executionId, e);
        }

        try {
            // 2. 删除 sys_job_runtime
            int rows = sysJobRuntimeMapper.deleteByExecutionId(executionId);
            log.info("[RUNTIME_DB_DELETE] jobId={} executionId={} rows={}", sysJob != null ? sysJob.getJobId() : "?", executionId, rows);
        } catch (Exception e) {
            log.error("[EXEC_CLEANUP_DB_FAIL] executionId={}", executionId, e);
        }

        try {
            // 3. 删除 Redis Keys
            String jobIdStr = (sysJob != null) ? String.valueOf(sysJob.getJobId()) : "*";
            redisTemplate.delete(RUNTIME_CACHE_PREFIX + executionId);
            redisTemplate.delete(DEDUP_KEY_PREFIX + jobIdStr);
            log.info("[DEDUP_LOCK_DEL] jobId={}", jobIdStr);
            log.info("[RUNTIME_CACHE_DEL] executionId={}", executionId);
        } catch (Exception e) {
             log.error("[EXEC_CLEANUP_REDIS_FAIL] executionId={}", executionId, e);
        }
    }

    /**
     * 执行恢复的任务 (供 TaskRecoveryService 调用)
     *
     * @param sysJob      恢复的任务
     * @param executionId 执行ID
     */
    public void executeRecoveredJob(SysJob sysJob, String executionId, Date scheduledTime) {
        if (sysJob == null || StringUtils.isEmpty(executionId)) {
            return;
        }

        MDC.put("traceId", executionId);
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMsg = null;
        String nodeId = NodeRegistry.getCurrentNodeId();

        try {
            // log.info("[RECOVER_START] jobId={} executionId={} target={}", sysJob.getJobId(), executionId, sysJob.getInvokeTarget());
            log.info("TASK_LIFECYCLE|CONSUME|jobId={}|executionId={}|nodeId={}|target={}|source=RECOVERY",
                    sysJob.getJobId(), executionId, nodeId, sysJob.getInvokeTarget());

            executeOnce(sysJob);
        } catch (Exception e) {
            status = "FAILED";
            errorMsg = StringUtils.substring(e.getMessage(), 0, 500);
            log.error("[RECOVER_FAIL] executionId={}", executionId, e);
            log.info("TASK_LIFECYCLE|FAIL|jobId={}|executionId={}|nodeId={}|costMs={}|errorMsg={}|source=RECOVERY",
                    sysJob.getJobId(), executionId, nodeId, (System.currentTimeMillis() - startTime), errorMsg);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // log.info("[RECOVER_END] jobId={} executionId={} status={} durationMs={}", sysJob.getJobId(), executionId, status, duration);
            if ("SUCCESS".equals(status)) {
                log.info("TASK_LIFECYCLE|COMPLETE|jobId={}|executionId={}|nodeId={}|costMs={}|source=RECOVERY",
                        sysJob.getJobId(), executionId, nodeId, duration);
            }

            // 最终清理
            handleFinalCleanup(executionId, sysJob, status, errorMsg, duration, scheduledTime != null ? scheduledTime : new Date());

            // 恢复任务成功后，也尝试调度下一次（如果它是个周期任务）
            // 这样能保证断链的周期任务继续跑
            if ("SUCCESS".equals(status)) {
                scheduleNextIfNeeded(sysJob, null, StringUtils.isEmpty(sysJob.getPriority()) ? "NORMAL" : sysJob.getPriority());
            }

            MDC.remove("traceId");
        }
    }

    /**
     * 执行一次任务（消费逻辑）
     */
    private void executeOnce(SysJob sysJob) throws Exception {
        // Requirement 2: 补充消费时过程日志
        String traceId = MDC.get("traceId");
        String threadName = Thread.currentThread().getName();
        long t1 = System.currentTimeMillis();

        log.info("[EXEC_INVOKE_START] Thread={} executionId={} target={}", threadName, traceId, sysJob.getInvokeTarget());
        try {
            JobInvokeUtil.invokeMethod(sysJob);
        } finally {
            long cost = System.currentTimeMillis() - t1;
            log.info("[EXEC_INVOKE_END] Thread={} executionId={} costMs={}", threadName, traceId, cost);
        }
    }

    /**
     * 如果是 cron 任务，计算下一次触发时间并入队
     */
    private void scheduleNextIfNeeded(SysJob sysJob, String targetNode, String priority) {
        // status=0 才继续（你项目常用：0正常 1暂停）
        if (!Objects.equals("0", sysJob.getStatus())) {
            return;
        }

        String cron = sysJob.getCronExpression();
        if (StringUtils.isEmpty(cron)) {
            return;
        }

        try {
            CronExpression ce = CronExpression.parse(cron);
            ZonedDateTime next = ce.next(ZonedDateTime.now(ZoneId.systemDefault()));
            if (next == null) {
                return;
            }

            long nextAt = next.toInstant().toEpochMilli();

            // 走统一 Producer Pipeline
            taskDistributor.scheduleJob(sysJob, nextAt);

        } catch (Exception e) {
            log.warn("[EXEC_NEXT_ERR] jobId={} cron={}", sysJob.getJobId(), cron, e);
        }
    }
}
