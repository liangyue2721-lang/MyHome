package com.make.quartz.bootstrap;

import com.make.quartz.config.QuartzProperties;
import com.make.quartz.service.impl.SysJobServiceImpl;
import com.make.quartz.util.NodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * JobBootstrapper
 *
 * <p>生产级启动自恢复入口：
 * <ul>
 *   <li>使用 SmartLifecycle 确保在 Spring 容器就绪后执行</li>
 *   <li>支持多节点：可选 Redis 分布式锁保证只有一个节点执行 bootstrap</li>
 *   <li>支持重试：等待 Redis/DB 等外部依赖短暂抖动后恢复</li>
 * </ul>
 */
@Component
public class JobBootstrapper implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(JobBootstrapper.class);

    private final QuartzProperties quartzProperties;
    private final SysJobServiceImpl sysJobService;
    private final RedisTemplate<String, String> redisTemplate;

    private volatile boolean running = false;

    public JobBootstrapper(
            QuartzProperties quartzProperties,
            SysJobServiceImpl sysJobService,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.quartzProperties = quartzProperties;
        this.sysJobService = sysJobService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;

        if (!quartzProperties.isBootstrapEnabled()) {
            log.info("[JOB_BOOTSTRAP_DISABLED] quartz.bootstrapEnabled=false");
            return;
        }

        String nodeId = NodeRegistry.getCurrentNodeId();
        String trigger = "smart-lifecycle";

        // 1) 分布式锁（可选）：只让一个节点做全量 bootstrap
        if (quartzProperties.isBootstrapUseRedisLock()) {
            boolean locked = tryAcquireBootstrapLock(nodeId);
            if (!locked) {
                log.info("[JOB_BOOTSTRAP_SKIP] nodeId={} reason=lock_not_acquired key={}",
                        nodeId, quartzProperties.getBootstrapLockKey());
                return;
            }
        }

        // 2) 重试：避免 Redis/DB 短暂抖动导致“启动无任务”
        int retryTimes = Math.max(1, quartzProperties.getBootstrapRetryTimes());
        long intervalMs = Math.max(500L, quartzProperties.getBootstrapRetryIntervalMs());

        for (int i = 1; i <= retryTimes; i++) {
            try {
                log.info("[JOB_BOOTSTRAP_TRY] nodeId={} attempt={}/{}", nodeId, i, retryTimes);
                sysJobService.bootstrapEnqueueEnabledJobs(trigger, false);
                log.info("[JOB_BOOTSTRAP_OK] nodeId={} attempt={}", nodeId, i);
                return;
            } catch (Exception e) {
                // 只要没到最后一次就继续重试
                if (i < retryTimes) {
                    log.warn("[JOB_BOOTSTRAP_RETRY] nodeId={} attempt={}/{} err={}",
                            nodeId, i, retryTimes, e.getMessage());
                    sleep(intervalMs);
                } else {
                    log.error("[JOB_BOOTSTRAP_GIVEUP] nodeId={} attempt={}/{}", nodeId, i, retryTimes, e);
                }
            }
        }
    }

    private boolean tryAcquireBootstrapLock(String nodeId) {
        String lockKey = quartzProperties.getBootstrapLockKey();
        long ttlSeconds = Math.max(5L, quartzProperties.getBootstrapLockTtlSeconds());

        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, nodeId, ttlSeconds, TimeUnit.SECONDS);
            return ok != null && ok;
        } catch (DataAccessException e) {
            // Redis 不可用时：为了避免“永远不启动”，降级为不加锁（依赖 TaskDistributor 的去重）
            log.warn("[JOB_BOOTSTRAP_LOCK_DEGRADED] nodeId={} reason=redis_error err={}",
                    nodeId, e.getMessage());
            return true;
        } catch (Exception e) {
            log.warn("[JOB_BOOTSTRAP_LOCK_DEGRADED] nodeId={} reason=unknown_err err={}",
                    nodeId, e.getMessage());
            return true;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // NodeRegistry / RedisMessageQueue 的 phase 是 Integer.MIN_VALUE
        // 这里设为略大，确保它们先启动（尽量避免启动时序抖动）
        return Integer.MIN_VALUE + 1000;
    }
}
