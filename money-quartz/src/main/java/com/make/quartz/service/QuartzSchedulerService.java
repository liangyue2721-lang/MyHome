package com.make.quartz.service;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.common.utils.uuid.IdUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.mapper.SysJobRuntimeMapper;
import com.make.quartz.util.NodeRegistry;
import com.make.quartz.util.RedisMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Quartz 调度服务（生产端）
 *
 * <p>职责：
 * 1) 接收任务触发请求
 * 2) 执行三段写（Redis Runtime -> DB WAITING -> Redis Queue）
 * 3) 处理去重与回滚
 */
@Service
public class QuartzSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(QuartzSchedulerService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysJobRuntimeMapper sysJobRuntimeMapper;

    private static final String DEDUP_KEY_PREFIX = "mq:job:dedup:";
    private static final String RUNTIME_CACHE_PREFIX = "mq:job:runtime:";

    /**
     * 调度任务（生成 executionId 并入队）
     *
     * @param sysJob 任务配置
     * @return executionId 如果入队成功；null 如果被去重或失败
     */
    public String scheduleJob(SysJob sysJob) {
        // 1. 生成 executionId (TraceId)
        String executionId = StringUtils.isNotEmpty(sysJob.getTraceId()) ? sysJob.getTraceId() : IdUtils.fastSimpleUUID();
        // 放入 MDC 方便日志追踪
        MDC.put("traceId", executionId);

        Long jobId = sysJob.getJobId();
        // 预估最大执行时长（TTL），默认 1 小时
        long ttlSeconds = 3600;

        // 2. Redis 去重 (SET NX EX)
        String dedupKey = DEDUP_KEY_PREFIX + jobId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(dedupKey, executionId, ttlSeconds, TimeUnit.SECONDS);

        if (locked == null || !locked) {
            log.info("[SCHED_DEDUP] Redis locked, skip. jobId={}, executionId={}", jobId, executionId);
            return null;
        }

        try {
            // 3. DB 兜底检查 (如果 Redis 刚过期但 DB 还在运行)
            // 只有 Redis 成功拿到锁才查 DB，减轻 DB 压力
            int runningOrWaiting = sysJobRuntimeMapper.countRunningOrWaiting(jobId);
            if (runningOrWaiting > 0) {
                log.info("[SCHED_SKIP] DB check found running/waiting task. jobId={}", jobId);
                // 回滚 Redis 锁
                redisTemplate.delete(dedupKey);
                return null;
            }

            // 4. 三段写 (Redis Runtime -> DB -> Enqueue)
            return performThreeStageWrite(sysJob, executionId, dedupKey);

        } catch (Exception e) {
            log.error("[SCHED_ERR] jobId={} executionId={}", jobId, executionId, e);
            // 异常回滚锁
            redisTemplate.delete(dedupKey);
            return null;
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 执行三段写逻辑
     */
    private String performThreeStageWrite(SysJob sysJob, String executionId, String dedupKey) {
        String runtimeCacheKey = RUNTIME_CACHE_PREFIX + executionId;

        // 4.1 写 Redis Runtime Cache
        SysJobRuntime runtime = new SysJobRuntime();
        runtime.setJobId(sysJob.getJobId());
        runtime.setJobName(sysJob.getJobName());
        runtime.setJobGroup(sysJob.getJobGroup());
        runtime.setExecutionId(executionId);
        runtime.setStatus("WAITING");
        runtime.setNodeId(null); // 尚未分配
        runtime.setEnqueueTime(new Date());
        runtime.setScheduledTime(new Date()); // 假设立即执行，如果是 delay 任务需要传参
        runtime.setRetryCount(0L);
        runtime.setMaxRetry(3L); // 默认
        runtime.setPayload(JSON.toJSONString(sysJob));

        redisTemplate.opsForValue().set(runtimeCacheKey, JSON.toJSONString(runtime), 1, TimeUnit.HOURS);

        try {
            // 4.2 插入 DB (sys_job_runtime) - 必须成功否则回滚
            sysJobRuntimeMapper.insertSysJobRuntime(runtime);
        } catch (Exception e) {
            log.error("[SCHED_DB_FAIL] Insert runtime failed. executionId={}", executionId, e);
            // 回滚 4.1 和 2
            redisTemplate.delete(runtimeCacheKey);
            redisTemplate.delete(dedupKey);
            return null;
        }

        try {
            // 4.3 Redis Enqueue (仅 executionId)
            // 修改：传入 executionId 到 sysJob 以便 RedisMessageQueue 使用
            sysJob.setTraceId(executionId);
            // 如果是 master node 任务，可能需要指定 targetNode。
            // 由于 NodeRegistry.getMasterNodeId() 不存在，且不引入新组件，
            // 这里暂不处理 Master 路由，或者默认由当前节点处理 (targetNode = null)
            String targetNode = null;

            RedisMessageQueue.getInstance().enqueueNow(sysJob, targetNode, sysJob.getPriority());

            log.info("[SCHED_SUCCESS] Enqueued. executionId={}", executionId);
            return executionId;

        } catch (Exception e) {
            log.error("[SCHED_ENQ_FAIL] Enqueue failed. executionId={}", executionId, e);
            // 4.3 失败：保留 Runtime 和 Dedup Key，等待补偿任务处理 (符合文档 4.3 失败处理)
            return null; // 虽然入队失败，但 DB 已有数据，视为“待补偿”状态
        }
    }
}
