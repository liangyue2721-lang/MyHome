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
import java.util.List;
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

    @Autowired
    private RedisMessageQueue redisMessageQueue;

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
        // Requirement 2: 补充任务生产请求日志
        log.info("[SCHEDULE_REQ] jobId={} scheduledAt={}", sysJob.getJobId(), scheduledAt);

        // 1. 生成 executionId
        String executionId = IdUtils.fastSimpleUUID();
        // 放入 MDC
        MDC.put("traceId", executionId);

        Long jobId = sysJob.getJobId();
        // 1.1 计算去重锁 TTL：默认 2 小时（兜底）
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
            // Redis 异常 -> 继续尝试，依靠 DB 唯一约束兜底
            log.error("[DEDUP_REDIS_ERR] jobId={} ex={} fallback=db_unique_constraint", jobId, e.getMessage());
        }

        // 3. 三段写
        return performThreeStageWrite(sysJob, executionId, scheduledAt, dedupKey);
    }

    /**
     * 批量调度任务（提升吞吐量）
     *
     * @param sysJobs     任务列表
     * @param scheduledAt 计划执行时间（毫秒），若为0或小于当前时间则视为立即执行
     * @return 成功调度的任务数
     */
    public int scheduleBatch(List<SysJob> sysJobs, long scheduledAt) {
        if (sysJobs == null || sysJobs.isEmpty()) {
            return 0;
        }

        // 1. 批量准备数据
        List<SysJobRuntime> insertList = new java.util.ArrayList<>();
        List<SysJob> validJobs = new java.util.ArrayList<>();
        List<String> validExecutionIds = new java.util.ArrayList<>();

        long now = System.currentTimeMillis();
        long dedupTtlSeconds = 7200;

        for (SysJob job : sysJobs) {
            String executionId = IdUtils.fastSimpleUUID();
            Long jobId = job.getJobId();
            String dedupKey = DEDUP_KEY_PREFIX + jobId;

            try {
                // Redis 去重 (简单循环，可优化为 Pipeline)
                // 这里为了简单稳健，仍然逐个 check，但因为去掉了 DB check，速度会很快
                Boolean locked = redisTemplate.opsForValue().setIfAbsent(dedupKey, executionId, dedupTtlSeconds, TimeUnit.SECONDS);
                if (locked == null || !locked) {
                    continue;
                }
            } catch (Exception e) {
                // Redis error, proceed to DB check via constraint
                log.error("[BATCH_DEDUP_ERR] jobId={} ex={}", jobId, e.getMessage());
            }

            // 构建 Runtime 对象
            SysJobRuntime runtime = new SysJobRuntime();
            runtime.setJobId(jobId);
            runtime.setJobName(job.getJobName());
            runtime.setJobGroup(job.getJobGroup());
            runtime.setExecutionId(executionId);
            runtime.setStatus("WAITING");
            runtime.setScheduledTime(scheduledAt > 0 ? new Date(scheduledAt) : new Date());
            runtime.setEnqueueTime(new Date());
            runtime.setRetryCount(0L);
            runtime.setMaxRetry(3L);
            runtime.setPayload(JSON.toJSONString(job));

            insertList.add(runtime);
            validJobs.add(job);
            validExecutionIds.add(executionId);

            // 记录 TraceId 供后续使用
            job.setTraceId(executionId);
        }

        if (insertList.isEmpty()) {
            return 0;
        }

        // 2. 批量写 DB
        try {
            // 这里假设 DB 有唯一约束 (job_id + status 组合 或类似)
            // 如果 Mybatis 批量插入遇到 Duplicate Key 会整个失败吗？
            // 通常是。如果为了极致稳定性，这里应该用 "INSERT IGNORE" 或 "ON DUPLICATE KEY UPDATE"
            // 但标准 insertSysJobRuntimeBatch 可能是普通 insert。
            // 鉴于我们已经过了 Redis 锁，冲突概率很低。如果冲突，这批任务失败，由重试机制处理。
            sysJobRuntimeMapper.insertSysJobRuntimeBatch(insertList);
            log.info("[BATCH_DB_INSERT] count={}", insertList.size());
        } catch (Exception e) {
            log.error("[BATCH_DB_FAIL] ex={}", e.getMessage());
            // 回滚 Redis 锁
            for (SysJob job : validJobs) {
                redisTemplate.delete(DEDUP_KEY_PREFIX + job.getJobId());
            }
            return 0;
        }

        // 3. 批量写 Cache & Enqueue (Pipeline)
        try {
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (int i = 0; i < validJobs.size(); i++) {
                    SysJob job = validJobs.get(i);
                    String execId = validExecutionIds.get(i);
                    SysJobRuntime rt = insertList.get(i);
                    String runtimeCacheKey = RUNTIME_CACHE_PREFIX + execId;

                    // 3.1 Cache
                    connection.setEx(
                            runtimeCacheKey.getBytes(),
                            86400, // 1 day
                            JSON.toJSONBytes(rt)
                    );

                    // 3.2 Enqueue
                    // Job with id = 30 is temporarily treated as highest priority.
                    // TODO: Remove this hardcoded logic once SysJob.priority is fully supported (DB + Mapper + UI).
                    String priority;
                    if (Long.valueOf(30L).equals(job.getJobId())) {
                        priority = "HIGH";
                    } else {
                        priority = StringUtils.isEmpty(job.getPriority()) ? "NORMAL" : job.getPriority();
                    }

                    // Use injected instance instead of getInstance() for better testability/pattern
                    // Force immediate enqueue regardless of scheduled time
                    redisMessageQueue.enqueueInPipeline(connection, job, execId, priority, 0);
                }
                return null;
            });

            // Log after pipeline submission (assumed success if no exception)
            for (int i = 0; i < validJobs.size(); i++) {
                log.info("TASK_LIFECYCLE|PRODUCE_BATCH|jobId={}|executionId={}", validJobs.get(i).getJobId(), validExecutionIds.get(i));
            }

            return validJobs.size();

        } catch (Exception e) {
            log.error("[BATCH_PIPELINE_FAIL] count={} ex={}", validJobs.size(), e.getMessage());
            // Partial failure handling in pipeline is tricky.
            // But since DB insert succeeded, consumers might pick it up from DB fallback eventually if cache missing.
            // Or simple retry logic could be added here.
            return 0;
        }
    }

    /**
     * 执行三段写（Runtime Cache -> DB -> Enqueue）
     */
    private String performThreeStageWrite(SysJob sysJob, String executionId, long scheduledAt, String dedupKey) {
        String runtimeCacheKey = RUNTIME_CACHE_PREFIX + executionId;

        // Requirement 2: 生产任务日志
        log.info("TASK_LIFECYCLE|PRODUCE|jobId={}|executionId={}|scheduledAt={}|dedupKey={}|target={}",
                sysJob.getJobId(), executionId, scheduledAt, dedupKey, sysJob.getInvokeTarget());

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
            // Requirement 2: 补充入队开始日志
            log.info("[MQ_ENQUEUE_START] jobId={} executionId={}", sysJob.getJobId(), executionId);

            // 绑定 executionId 到任务对象，以便 Queue 使用
            sysJob.setTraceId(executionId);

            // 默认优先级
            // Job with id = 30 is temporarily treated as highest priority.
            // TODO: Remove this hardcoded logic once SysJob.priority is fully supported (DB + Mapper + UI).
            String priority;
            if (Long.valueOf(30L).equals(sysJob.getJobId())) {
                priority = "HIGH";
            } else {
                priority = StringUtils.isEmpty(sysJob.getPriority()) ? "NORMAL" : sysJob.getPriority();
            }

            // 路由策略：目前默认当前节点或由 Queue 内部处理 (targetNode=null)
            String targetNode = null;

            // 修改调度逻辑：所有任务（无论计划时间是否已到）均视为具备执行条件，直接入列
            // 当节点空闲时，可提前抢占执行 WAITING 状态的任务
            // 注：scheduledAt 仅作为记录使用，不再推迟入队
            RedisMessageQueue.getInstance().enqueueNow(sysJob, targetNode, priority);

            // 补充：Redis 入队成功后，更新 DB 入队时间 (使用 DB NOW())
            try {
                sysJobRuntimeMapper.updateEnqueueTime(executionId);
            } catch (Exception ex) {
                log.warn("[ENQUEUE_TIME_UPDATE_FAIL] executionId={} ex={}", executionId, ex.getMessage());
                // 不阻断流程，仅记录警告
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
