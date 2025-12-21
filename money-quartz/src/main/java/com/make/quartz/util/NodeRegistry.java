package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 节点注册与心跳管理器 (Node Registry)
 *
 * 职责：
 * 1. 注册当前节点到 Redis Set (`SCHEDULER_NODES`)
 * 2. 定期上报心跳和负载指标 (Memory, Load) 到 Redis
 *
 * 日志说明：
 * - [NODE_INIT]: 节点初始化
 * - [NODE_HEARTBEAT]: 心跳上报成功 (DEBUG级别，含指标数据)
 * - [NODE_METRICS_ERROR]: 指标收集失败
 */
@Component
public class NodeRegistry implements SmartLifecycle {
    
    private static final Logger log = LoggerFactory.getLogger(NodeRegistry.class);
    
    private static final String SCHEDULER_NODES_KEY = "SCHEDULER_NODES";
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:";
    private static final String SCHEDULER_NODE_METRICS_SUFFIX = ":METRICS";
    
    private static final String CURRENT_NODE_ID = IpUtils.getHostIp() + ":" + UUID.randomUUID().toString();
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private ScheduledExecutorService heartbeatScheduler;
    private volatile boolean active = false;
    private volatile boolean isRunning = false;
    
    @Override
    public void start() {
        if (isRunning) {
            return;
        }
        active = true;
        isRunning = true;
        registerNode();
        startHeartbeat();
        log.info("[NODE_INIT] 节点注册完成 | ID: {}", CURRENT_NODE_ID);
    }

    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }
        destroy();
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        // Integer.MIN_VALUE means start first
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    private void registerNode() {
        try {
            if (!active) return;
            redisTemplate.opsForSet().add(SCHEDULER_NODES_KEY, CURRENT_NODE_ID);
            redisTemplate.expire(SCHEDULER_NODES_KEY, 300, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[NODE_REG_ERROR] 节点注册失败", e);
        }
    }
    
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NodeHeartbeat");
            t.setDaemon(true);
            return t;
        });
        
        heartbeatScheduler.scheduleAtFixedRate(this::updateHeartbeat, 0, 30, TimeUnit.SECONDS);
    }
    
    private void updateHeartbeat() {
        if (!active || Thread.currentThread().isInterrupted()) {
            return;
        }
        try {
            Map<String, Object> metrics = collectNodeMetrics();
            String metricsJson = JSON.toJSONString(metrics);
            
            String metricsKey = SCHEDULER_NODE_PREFIX + CURRENT_NODE_ID + SCHEDULER_NODE_METRICS_SUFFIX;
            redisTemplate.opsForValue().set(metricsKey, metricsJson);
            redisTemplate.expire(metricsKey, 300, TimeUnit.SECONDS);
            
            // 刷新节点列表过期时间
            redisTemplate.expire(SCHEDULER_NODES_KEY, 300, TimeUnit.SECONDS);

            if (log.isDebugEnabled()) {
                log.debug("[NODE_HEARTBEAT] 心跳上报成功 | Metrics: {}", metricsJson);
            }
        } catch (org.springframework.data.redis.RedisConnectionFailureException | org.springframework.data.redis.RedisSystemException e) {
            if (!active) {
                log.debug("[NODE_HEARTBEAT_SKIP] shutdown in progress, skip heartbeat");
                return;
            }
            throw e;
        } catch (Exception e) {
            if (!active) {
                log.debug("[NODE_HEARTBEAT_SKIP] shutdown exception suppressed: {}", e.getClass().getSimpleName());
                return;
            }
            log.error("[NODE_HEARTBEAT_ERROR] heartbeat failed", e);
        }
    }
    
    private Map<String, Object> collectNodeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", System.currentTimeMillis());

        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) heapUsed / heapMax;
            metrics.put("memoryUsage", memoryUsage);

            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double systemLoad = osBean.getSystemLoadAverage();
            metrics.put("systemLoad", systemLoad);
            
        } catch (Exception e) {
            log.warn("[NODE_METRICS_ERROR] 收集节点指标异常", e);
        }

        return metrics;
    }
    
    public static String getCurrentNodeId() {
        return CURRENT_NODE_ID;
    }
    
    public void destroy() {
        this.active = false;
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        
        try {
            redisTemplate.opsForSet().remove(SCHEDULER_NODES_KEY, CURRENT_NODE_ID);
            String metricsKey = SCHEDULER_NODE_PREFIX + CURRENT_NODE_ID + SCHEDULER_NODE_METRICS_SUFFIX;
            redisTemplate.delete(metricsKey);
            log.info("[NODE_DESTROY] 节点注销完成");
        } catch (Exception e) {
            // 忽略销毁时的异常
            log.info("[NODE_DESTROY] 节点注销过程异常(可忽略): {}", e.getMessage());
        }
    }
}
