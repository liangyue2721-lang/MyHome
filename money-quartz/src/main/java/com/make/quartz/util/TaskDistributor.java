package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.common.utils.uuid.IdUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.mapper.SysJobRuntimeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 任务分发器（统一生产侧 Pipeline）
 *
 * <p>职责：
 * 1. 接收调度请求（立即/延时/周期）
 * 2. 执行严格的“Redis去重 -> DB兜底 -> 三段写”流程
 * 3. 统一入队入口
 */
@Component
public class TaskDistributor {

    private static final Logger log = LoggerFactory.getLogger(TaskDistributor.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysJobRuntimeMapper sysJobRuntimeMapper;

    private static final String DEDUP_KEY_PREFIX = "mq:job:dedup:";
    private static final String RUNTIME_CACHE_PREFIX = "mq:job:runtime:";

    /**
     * 调度任务（统一入口）
     *
     * @param sysJob      任务定义
     * @param scheduledAt 计划执行时间（毫秒），若为0或小于当前时间则视为立即执行
     * @return executionId 若调度成功；null 若被去重或失败
     */
    public String scheduleJob(SysJob sysJob, long scheduledAt) {
        // 1. 生成 executionId
        String executionId = IdUtils.fastSimpleUUID();
        // 放入 MDC
        MDC.put("traceId", executionId);

        Long jobId = sysJob.getJobId();
        // 1.1 计算去重锁 TTL：默认 2 小时（兜底）
        // 理想情况应取 max(avgDuration, interval)，这里简化为固定兜底
        long dedupTtlSeconds = 7200;

        // 2. Redis 去重 (SET NX)
        String dedupKey = DEDUP_KEY_PREFIX + jobId;
        try {
            log.info("[DEDUP_LOCK_TRY] jobId={} ttlSec={} scheduledAt={}", jobId, dedupTtlSeconds, scheduledAt);
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(dedupKey, executionId, dedupTtlSeconds, TimeUnit.SECONDS);

            if (locked == null || !locked) {
                // 锁存在
                log.info("[DEDUP_LOCK_HIT] jobId={} reason=unfinished", jobId);
                return null;
            }
            log.info("[DEDUP_LOCK_OK] jobId={} executionId={} ttlSec={}", jobId, executionId, dedupTtlSeconds);

        } catch (Exception e) {
            // Redis 异常 -> DB 兜底
            log.error("[DEDUP_REDIS_ERR] jobId={} ex={} fallback=db", jobId, e.getMessage());
            try {
                int unfinished = sysJobRuntimeMapper.countRunningOrWaiting(jobId);
                if (unfinished > 0) {
                    log.info("[DEDUP_DB_HIT] jobId={} unfinishedCount={}", jobId, unfinished);
                    return null;
                }
            } catch (Exception dbEx) {
                log.error("[DEDUP_DB_ERR] jobId={} ex={}", jobId, dbEx.getMessage());
                return null; // DB 也挂了，放弃以防雪崩
            }
        }

        // 3. 三段写
        return performThreeStageWrite(sysJob, executionId, scheduledAt, dedupKey);
    }

    /**
     * 执行三段写（Runtime Cache -> DB -> Enqueue）
     */
    private String performThreeStageWrite(SysJob sysJob, String executionId, long scheduledAt, String dedupKey) {
        String runtimeCacheKey = RUNTIME_CACHE_PREFIX + executionId;

        // 3.1 写 Redis Runtime Cache (TTL 1 Day)
        SysJobRuntime runtime = new SysJobRuntime();
        runtime.setJobId(sysJob.getJobId());
        runtime.setJobName(sysJob.getJobName());
        runtime.setJobGroup(sysJob.getJobGroup());
        runtime.setExecutionId(executionId);
        runtime.setStatus("WAITING");
        runtime.setScheduledTime(scheduledAt > 0 ? new Date(scheduledAt) : new Date());
        runtime.setEnqueueTime(new Date());
        runtime.setRetryCount(0L);
        runtime.setMaxRetry(3L);
        runtime.setPayload(JSON.toJSONString(sysJob));

        try {
            redisTemplate.opsForValue().set(runtimeCacheKey, JSON.toJSONString(runtime), 1, TimeUnit.DAYS);
            log.info("[RUNTIME_CACHE_SET] jobId={} executionId={} status=WAITING ttl=86400", sysJob.getJobId(), executionId);
        } catch (Exception e) {
            log.error("[RUNTIME_CACHE_FAIL] executionId={}", executionId, e);
            // Cache 写失败是否阻断？
            // 策略：Best effort. 如果 Cache 失败，DB 成功，Consumer 还可以查 DB。
            // 但如果 Redis 全挂，Enqueue 也会挂。
            // 这里暂且继续，依靠 DB 兜底。
        }

        // 3.2 写 DB (sys_job_runtime) - 必须成功否则回滚
        try {
            int rows = sysJobRuntimeMapper.insertSysJobRuntime(runtime);
            log.info("[RUNTIME_DB_INSERT] jobId={} executionId={} rows={}", sysJob.getJobId(), executionId, rows);
        } catch (Exception e) {
            log.error("[RUNTIME_DB_FAIL] Insert runtime failed. executionId={}", executionId, e);
            // 回滚 Redis 锁 & Cache
            redisTemplate.delete(dedupKey);
            redisTemplate.delete(runtimeCacheKey);
            return null;
        }

        // 3.3 Redis Enqueue
        try {
            // 绑定 executionId 到任务对象，以便 Queue 使用
            sysJob.setTraceId(executionId);

            // 默认优先级
            String priority = StringUtils.isEmpty(sysJob.getPriority()) ? "NORMAL" : sysJob.getPriority();

            // 路由策略：目前默认当前节点或由 Queue 内部处理 (targetNode=null)
            String targetNode = null;

            // 区分立即执行还是延迟执行
            long now = System.currentTimeMillis();
            if (scheduledAt <= now) {
                RedisMessageQueue.getInstance().enqueueNow(sysJob, targetNode, priority);
            } else {
                RedisMessageQueue.getInstance().enqueueAt(sysJob, targetNode, priority, scheduledAt);
            }

            log.info("[MQ_ENQUEUE] jobId={} executionId={} scheduledAt={}", sysJob.getJobId(), executionId, scheduledAt);
            return executionId;

        } catch (Exception e) {
            log.error("[MQ_ENQUEUE_FAIL] executionId={}", executionId, e);
            // 3.3 失败处理：
            // 按文档：保留 Runtime 和 Dedup Key，等待补偿任务处理。
            // 所以这里不回滚 DB/Redis。
            return null;
        } finally {
            MDC.remove("traceId");
        }
    }
}
