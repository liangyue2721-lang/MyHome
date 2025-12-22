package com.make.quartz.service;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobExecutionLog;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.mapper.SysJobExecutionLogMapper;
import com.make.quartz.mapper.SysJobRuntimeMapper;
import com.make.quartz.util.JobInvokeUtil;
import com.make.quartz.util.NodeRegistry;
import com.make.quartz.util.RedisMessageQueue;
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
    private QuartzSchedulerService quartzSchedulerService;

    private static final String DEDUP_KEY_PREFIX = "mq:job:dedup:";
    private static final String RUNTIME_CACHE_PREFIX = "mq:job:runtime:";

    /**
     * 防止同 executionId 重入
     */
    private final ConcurrentHashMap<String, Long> executing = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String nodeId = NodeRegistry.getCurrentNodeId();
        RedisMessageQueue.getInstance().startListening(nodeId, this::handle);
    }


    @PreDestroy
    public void destroy() {
        RedisMessageQueue.getInstance().stopListening();
    }

    /**
     * 处理一条队列消息
     */
    private void handle(RedisMessageQueue.TaskMessage message) throws Exception {
        // 0. 特殊处理 TRIGGER 消息
        // 如果是 TRIGGER 类型，表示这是一个“定时触发信号”，不是直接执行的任务。
        // 需要调用 QuartzSchedulerService 进行正式调度（三段写）
        if (RedisMessageQueue.TaskMessage.TYPE_TRIGGER.equals(message.getMessageType())) {
            SysJob sysJob = (SysJob) message.getJobData();
            if (sysJob != null) {
                // 将 jobGroup 还原 (如果之前被临时改了，这里其实拿到的是 JSON 反序列化的副本，不影响配置)
                // 注意：我们在 scheduleNextIfNeeded 里可能改了 group，这里恢复或者直接用
                // 更好的方式是依靠 messageType 判断，不依赖 group
                // 重新调度：生成新 executionId -> Dedup -> DB -> Queue
                quartzSchedulerService.scheduleJob(sysJob);
                log.info("[EXEC_TRIGGER] Triggered new schedule for jobId={}", sysJob.getJobId());
            }
            return; // 触发完毕，ACK
        }

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

            // 3. 执行任务
            SysJob sysJob = (SysJob) message.getJobData();
            // 如果 message 中只有 ID (符合新规范)，则需要回查 SysJobRuntime
            if (sysJob == null) {
                // 尝试从 Runtime Cache 或 DB 获取
                 String runtimeJson = redisTemplate.opsForValue().get(RUNTIME_CACHE_PREFIX + execId);
                 if (StringUtils.isNotEmpty(runtimeJson)) {
                     SysJobRuntime runtime = JSON.parseObject(runtimeJson, SysJobRuntime.class);
                     sysJob = JSON.parseObject(runtime.getPayload(), SysJob.class);
                 } else {
                     SysJobRuntime runtime = sysJobRuntimeMapper.selectSysJobRuntimeByExecutionId(execId);
                     if (runtime != null) {
                         sysJob = JSON.parseObject(runtime.getPayload(), SysJob.class);
                     }
                 }
            }

            if (sysJob == null || StringUtils.isEmpty(sysJob.getInvokeTarget())) {
                log.warn("[EXEC_DROP] invalid message/payload missing, executionId={}", execId);
                // 无法执行，标记失败并清理
                handleFinalCleanup(execId, null, "FAILED", "Missing payload", 0L);
                return;
            }

            long startTime = System.currentTimeMillis();
            String status = "SUCCESS";
            String errorMsg = null;

            try {
                log.info("[EXEC_START] executionId={} target={}", execId, sysJob.getInvokeTarget());
                executeOnce(sysJob);
            } catch (Exception e) {
                status = "FAILED";
                errorMsg = StringUtils.substring(e.getMessage(), 0, 500);
                log.error("[EXEC_FAIL] executionId={}", execId, e);
            } finally {
                long duration = System.currentTimeMillis() - startTime;

                // 4. 结束尽力而为清理 (Insert Log -> Delete Runtime -> Delete Redis)
                handleFinalCleanup(execId, sysJob, status, errorMsg, duration);
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
    private void handleFinalCleanup(String executionId, SysJob sysJob, String status, String errorMsg, Long duration) {
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

            sysJobExecutionLogMapper.insertSysJobExecutionLog(logEntry);

        } catch (Exception e) {
            log.error("[EXEC_CLEANUP_LOG_FAIL] executionId={}", executionId, e);
        }

        try {
            // 2. 删除 sys_job_runtime
            sysJobRuntimeMapper.deleteByExecutionId(executionId);
        } catch (Exception e) {
            log.error("[EXEC_CLEANUP_DB_FAIL] executionId={}", executionId, e);
        }

        try {
            // 3. 删除 Redis Keys
            String jobIdStr = (sysJob != null) ? String.valueOf(sysJob.getJobId()) : "*";
            redisTemplate.delete(RUNTIME_CACHE_PREFIX + executionId);
            redisTemplate.delete(DEDUP_KEY_PREFIX + jobIdStr);
        } catch (Exception e) {
             log.error("[EXEC_CLEANUP_REDIS_FAIL] executionId={}", executionId, e);
        }
    }

    /**
     * 执行一次任务（消费逻辑）
     */
    private void executeOnce(SysJob sysJob) throws Exception {
        JobInvokeUtil.invokeMethod(sysJob);
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

            // 构造一个“自触发”消息
            // 注意：这里我们传入 sysJob，但它会在 RedisMessageQueue 里被设置为 TRIGGER 类型
            // 我们必须确保 RedisMessageQueue 知道这是一个 TRIGGER。
            // 我们在 RedisMessageQueue.enqueueAt 里加了逻辑：如果 group 是 "QUARTZ_INTERNAL_TRIGGER"，则设为 TRIGGER。
            // 所以这里我们必须 clone 并 setGroup。

            SysJob nextJob = JSON.parseObject(JSON.toJSONString(sysJob), SysJob.class);
            nextJob.setJobGroup("QUARTZ_INTERNAL_TRIGGER");
            nextJob.setTraceId(null); // 清除旧 TraceId，让 RedisQueue 或 Scheduler 生成新的

            RedisMessageQueue.getInstance().enqueueAt(nextJob, targetNode, priority, nextAt);

            log.info("[EXEC_NEXT] jobId={} nextAt={}", sysJob.getJobId(), Instant.ofEpochMilli(nextAt));
        } catch (Exception e) {
            log.warn("[EXEC_NEXT_ERR] jobId={} cron={}", sysJob.getJobId(), cron, e);
        }
    }
}
