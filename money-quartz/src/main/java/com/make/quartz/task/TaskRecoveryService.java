package com.make.quartz.task;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.service.ISysJobRuntimeService;
import com.make.quartz.service.TaskExecutionService;
import com.make.quartz.util.NodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务恢复服务 (Requirement 1: 启动后/定期检查补偿机制)
 *
 * <p>定期检查 sys_job_runtime 中的活跃任务，如果 Redis 锁已丢失（Dedup Key missing），
 * 则认为该任务处于“僵死”或“丢失”状态，需要手动提交到线程池恢复执行。
 */
@Component("Task-Recovery-Service")
public class TaskRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryService.class);

    @Resource
    private ISysJobRuntimeService sysJobRuntimeService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private TaskExecutionService taskExecutionService;

    private static final String DEDUP_KEY_PREFIX = "mq:job:dedup:";
    private static final String RUNTIME_CACHE_PREFIX = "mq:job:runtime:";
    private static final String TASK_MONITOR_PREFIX = "TASK_MONITOR:";

    private volatile boolean running = true;
    private Thread recoveryThread;

    /**
     * Requirement 1: 启动时立刻执行一次任务恢复扫描
     */
    @PostConstruct
    public void init() {
        log.info("[RECOVERY_INIT] Starting initial task recovery check...");

        // 1. 立即执行一次本地恢复
        recoverLocalZombieTasks();
        recoverLostTasks();

        // 2. 启动后台线程定期执行恢复
        recoveryThread = new Thread(() -> {
            log.info("[RECOVERY_THREAD] Starting background recovery thread...");
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // 每30秒执行一次
                    Thread.sleep(30000);
                    recoverLostTasks();
                } catch (InterruptedException e) {
                    log.info("[RECOVERY_THREAD] Thread interrupted, stopping...");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("[RECOVERY_THREAD] Error in recovery loop", e);
                    // 避免死循环狂刷日志
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            log.info("[RECOVERY_THREAD] Thread stopped.");
        }, "TaskRecovery-Thread");

        recoveryThread.setDaemon(true);
        recoveryThread.start();
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (recoveryThread != null) {
            recoveryThread.interrupt();
        }
    }

    /**
     * 恢复本地僵尸任务
     * 处理场景：应用崩溃/非优雅关闭重启后，sys_job_runtime 中仍有 RUNNING 状态且 node_id 为本机的任务
     */
    private void recoverLocalZombieTasks() {
        try {
            String currentNodeId = NodeRegistry.getCurrentNodeId();

            // 查询所有 active 任务，然后过滤
            // (Mapper 没有直接支持 where node_id and status，为了稳妥先 selectAllActive 过滤，或者构造 Example 如果支持)
            // SysJobRuntimeMapper.selectActiveJobs() -> where status in ('WAITING', 'RUNNING')

            // 更精准的做法：使用 selectSysJobRuntimeList
            SysJobRuntime query = new SysJobRuntime();
            query.setNodeId(currentNodeId);
            query.setStatus("RUNNING");
            List<SysJobRuntime> localRunningTasks = sysJobRuntimeService.selectSysJobRuntimeList(query);

            if (localRunningTasks == null || localRunningTasks.isEmpty()) {
                return;
            }

            log.info("[RECOVERY_ZOMBIE] Found {} local zombie tasks from previous run. nodeId={}", localRunningTasks.size(), currentNodeId);

            for (SysJobRuntime task : localRunningTasks) {
                try {
                    Long jobId = task.getJobId();
                    String execId = task.getExecutionId();

                    // 1. 清理 Redis 锁，确保 recoverLostTasks 能识别到它（missing key）
                    redisTemplate.delete(DEDUP_KEY_PREFIX + jobId);
                    redisTemplate.delete(RUNTIME_CACHE_PREFIX + execId);
                    redisTemplate.delete(TASK_MONITOR_PREFIX + jobId);

                    // 2. 重置状态为 WAITING
                    task.setStatus("WAITING");
                    task.setNodeId(null);
                    sysJobRuntimeService.updateSysJobRuntime(task);

                    log.info("[RECOVERY_ZOMBIE_RESET] jobId={} execId={} -> WAITING. Ready for recovery loop.", jobId, execId);

                } catch (Exception e) {
                    log.error("[RECOVERY_ZOMBIE_ERR] execId={} err={}", task.getExecutionId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[RECOVERY_ZOMBIE_FATAL] Error checking local zombie tasks", e);
        }
    }

    /**
     * 定期检查丢失任务并恢复 (30秒)
     */
    public void recoverLostTasks() {
        try {
            log.info("[RECOVERY_LOOP_START] Checking for lost tasks...");
            // 1. 查询所有活跃任务 (WAITING / RUNNING)
            List<SysJobRuntime> activeJobs = sysJobRuntimeService.selectActiveJobs();
            if (activeJobs == null || activeJobs.isEmpty()) {
                return;
            }

            int recoveredCount = 0;

            for (SysJobRuntime runtime : activeJobs) {
                try {
                    Long jobId = runtime.getJobId();
                    String executionId = runtime.getExecutionId();
                    String dedupKey = DEDUP_KEY_PREFIX + jobId;

                    // 2. 检查 Redis Dedup Key 是否丢失
                    // 判定依据：Redis 无，DB 有 (Active)
                    if (Boolean.FALSE.equals(redisTemplate.hasKey(dedupKey))) {
                        log.warn("[RECOVERY_TRIGGER] Found zombie task: jobId={} executionId={}. Redis key missing.", jobId, executionId);
                        recoveredCount++;

                        // 3. 恢复 Redis 锁 (防止重复恢复 & 满足生产去重规则)
                        // 2小时 TTL
                        redisTemplate.opsForValue().set(dedupKey, executionId, 2, TimeUnit.HOURS);

                        // 4. 解析 Payload
                        String payload = runtime.getPayload();
                        if (StringUtils.isEmpty(payload)) {
                            log.error("[RECOVERY_FAIL] Empty payload for executionId={}", executionId);
                            // 无法恢复，清理脏数据？暂不清理，避免误删
                            continue;
                        }
                        SysJob sysJob = JSON.parseObject(payload, SysJob.class);

                        // 5. 提交到 股票更新线程池 (WATCH_STOCK_EXECUTOR) 进行执行
                        ThreadPoolUtil.getWatchStockExecutor().submit(() -> {
                            try {
                                log.info("[RECOVERY_EXEC] Submitting recovered task to thread pool. jobId={}", jobId);
                                // Updated to pass scheduledTime
                                taskExecutionService.executeRecoveredJob(sysJob, executionId, runtime.getScheduledTime());
                            } catch (Exception e) {
                                log.error("[RECOVERY_EXEC_ERR] jobId={} executionId={}", jobId, executionId, e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("[RECOVERY_ITEM_ERR] executionId={} err={}", runtime.getExecutionId(), e.getMessage());
                }
            }

            if (recoveredCount > 0) {
                 log.info("[RECOVERY_SUMMARY] Recovered {} tasks.", recoveredCount);
            }

        } catch (Exception e) {
            log.error("[RECOVERY_LOOP_ERR] Error in recovery loop", e);
        }
    }
}
