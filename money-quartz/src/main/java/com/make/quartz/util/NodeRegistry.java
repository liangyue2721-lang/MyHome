package com.make.quartz.util;

import com.make.common.utils.StringUtils;
import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 节点注册中心（安全版）
 *
 * <p>设计原则：
 * - 启动阶段：不依赖 HTTP 请求上下文
 * - 运行阶段：如有请求上下文，可使用请求 IP（可选）
 * - nodeId 在进程生命周期内保持稳定
 */
@Component
public class NodeRegistry implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(NodeRegistry.class);

    private static final String NODE_SET_KEY = "mq:nodes:alive";
    private static final String NODE_TTL_PREFIX = "mq:nodes:ttl:";

    /**
     * 进程级稳定 nodeId
     */
    private static volatile String CURRENT_NODE_ID;

    private final RedisTemplate<String, String> redisTemplate;

    private volatile boolean running = false;
    private ScheduledExecutorService heartbeatScheduler;

    public NodeRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取当前节点 ID
     *
     * <p>规则：
     * 1. 优先使用本机 IP (Requirement 4: 记录执行的当前机器IP)
     * 2. 如果无法获取，使用兜底逻辑
     */
    public static String getCurrentNodeId() {
        if (CURRENT_NODE_ID != null) {
            return CURRENT_NODE_ID;
        }

        synchronized (NodeRegistry.class) {
            if (CURRENT_NODE_ID != null) {
                return CURRENT_NODE_ID;
            }

            // ===== 1️⃣ 优先使用本机 IP =====
            try {
                String ip = IpUtils.getHostIp();
                if (StringUtils.isNotEmpty(ip) && !"127.0.0.1".equals(ip)) {
                    CURRENT_NODE_ID = ip;
                } else {
                     // 尝试其他方式获取非 loopback IP
                     String ipAddr = IpUtils.getIpAddr();
                     if (StringUtils.isNotEmpty(ipAddr) && !"unknown".equalsIgnoreCase(ipAddr)) {
                         CURRENT_NODE_ID = ipAddr;
                     } else {
                         CURRENT_NODE_ID = ip; // Fallback to 127.0.0.1
                     }
                }
            } catch (Exception e) {
                CURRENT_NODE_ID = "127.0.0.1";
            }

            log.info("[NODE_ID_INIT] nodeId={}", CURRENT_NODE_ID);
            return CURRENT_NODE_ID;
        }
    }

    @PostConstruct
    public void init() {
        // 初始化 nodeId（确保启动阶段就确定）
        getCurrentNodeId();
    }

    @Override
    public void start() {
        this.running = true;
        startHeartbeat();
    }

    private void startHeartbeat() {
        if (heartbeatScheduler == null || heartbeatScheduler.isShutdown()) {
            heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "node-heartbeat-thread");
                t.setDaemon(true);
                return t;
            });
        }

        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            String nodeId = getCurrentNodeId();
            try {
                // Refresh TTL
                redisTemplate.opsForValue().set(
                        NODE_TTL_PREFIX + nodeId,
                        "1",
                        30,
                        TimeUnit.SECONDS
                );
                // Ensure in Set (in case accidentally removed)
                redisTemplate.opsForSet().add(NODE_SET_KEY, nodeId);
            } catch (Exception e) {
                // Suppress errors during shutdown or connection failure to avoid noise
                if (running) {
                    log.warn("[NODE_HEARTBEAT_FAIL] nodeId={} msg={}", nodeId, e.getMessage());
                }
            }
        }, 0, 10, TimeUnit.SECONDS);

        log.info("[NODE_REGISTER] Heartbeat started. nodeId={}", getCurrentNodeId());
    }

    @Override
    public void stop() {
        this.running = false;
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        log.info("[NODE_REGISTER] Stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 尽量早于 TaskExecutionService
        return Integer.MIN_VALUE;
    }
}
