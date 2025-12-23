package com.make.quartz.task;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.mapper.SysJobRuntimeMapper;
import com.make.quartz.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务恢复服务 (Requirement 1: 启动后/定期检查补偿机制)
 *
 * <p>定期检查 sys_job_runtime 中的活跃任务，如果 Redis 锁已丢失（Dedup Key missing），
 * 则认为该任务处于“僵死”或“丢失”状态，需要手动提交到线程池恢复执行。
 */
@Component
public class TaskRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryService.class);

    @Autowired
    private SysJobRuntimeMapper sysJobRuntimeMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private static final String DEDUP_KEY_PREFIX = "mq:job:dedup:";

    /**
     * 定期检查丢失任务并恢复 (30秒)
     */
    @Scheduled(fixedDelay = 30000)
    public void recoverLostTasks() {
        try {
            // 1. 查询所有活跃任务 (WAITING / RUNNING)
            List<SysJobRuntime> activeJobs = sysJobRuntimeMapper.selectActiveJobs();
            if (activeJobs == null || activeJobs.isEmpty()) {
                return;
            }

            for (SysJobRuntime runtime : activeJobs) {
                try {
                    Long jobId = runtime.getJobId();
                    String executionId = runtime.getExecutionId();
                    String dedupKey = DEDUP_KEY_PREFIX + jobId;

                    // 2. 检查 Redis Dedup Key 是否丢失
                    // 判定依据：Redis 无，DB 有 (Active)
                    if (Boolean.FALSE.equals(redisTemplate.hasKey(dedupKey))) {
                        log.warn("[RECOVERY_TRIGGER] Found zombie task: jobId={} executionId={}. Redis key missing.", jobId, executionId);

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
                                taskExecutionService.executeRecoveredJob(sysJob, executionId);
                            } catch (Exception e) {
                                log.error("[RECOVERY_EXEC_ERR] jobId={} executionId={}", jobId, executionId, e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("[RECOVERY_ITEM_ERR] executionId={} err={}", runtime.getExecutionId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[RECOVERY_LOOP_ERR] Error in recovery loop", e);
        }
    }
}
