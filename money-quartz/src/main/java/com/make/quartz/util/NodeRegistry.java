package com.make.quartz.util;

import com.make.common.utils.StringUtils;
import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
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
 * - 启动阶段：优先读取配置ID，无配置则自动探测IP
 * - 运行阶段：保持ID稳定
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

    @Autowired
    private Environment environment;

    private volatile boolean running = false;
    private ScheduledExecutorService heartbeatScheduler;

    public NodeRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取当前节点 ID
     *
     * <p>规则：
     * 1. 强制使用本机 IP (Hosted IP)
     * 2. 兜底 127.0.0.1
     */
    public static String getCurrentNodeId() {
        if (CURRENT_NODE_ID != null) {
            return CURRENT_NODE_ID;
        }
        // Initializing early might miss Spring Context injection if called statically before init
        // But the init() method below ensures it is set.
        return "UNKNOWN";
    }

    @PostConstruct
    public void init() {
        // Enforce IP-only identity
        try {
            String ip = IpUtils.getHostIp();
            if (StringUtils.isNotEmpty(ip) && !"127.0.0.1".equals(ip)) {
                CURRENT_NODE_ID = ip;
            } else {
                String ipAddr = IpUtils.getIpAddr();
                if (StringUtils.isNotEmpty(ipAddr) && !"unknown".equalsIgnoreCase(ipAddr)) {
                    CURRENT_NODE_ID = ipAddr;
                } else {
                    CURRENT_NODE_ID = "127.0.0.1";
                }
            }
        } catch (Exception e) {
            CURRENT_NODE_ID = "127.0.0.1";
        }
        log.info("[NODE_ID_INIT] using ip id={}", CURRENT_NODE_ID);
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
            if ("UNKNOWN".equals(nodeId)) {
                 // Retry init if still unknown (unlikely if @PostConstruct ran)
                 init();
                 nodeId = getCurrentNodeId();
                 if ("UNKNOWN".equals(nodeId)) {
                     log.warn("[NODE_ID_UNKNOWN] Still unable to determine nodeId.");
                 }
            }

            try {
                // Refresh TTL
                redisTemplate.opsForValue().set(
                        NODE_TTL_PREFIX + nodeId,
                        "1",
                        30,
                        TimeUnit.SECONDS
                );
                // Ensure in Set (in case accidentally removed)
                Long added = redisTemplate.opsForSet().add(NODE_SET_KEY, nodeId);
                if (added != null) {
                    if (added > 0) {
                        log.info("[NODE_REGISTER_SUCCESS] nodeId={}", nodeId);
                    } else if (log.isDebugEnabled()) {
                        log.debug("[NODE_REFRESH] nodeId={} refreshed.", nodeId);
                    }
                }
            } catch (Throwable e) {
                // Suppress errors during shutdown or connection failure to avoid noise
                if (running) {
                    log.warn("[NODE_HEARTBEAT_FAIL] nodeId={} msg={}", nodeId, e.getMessage(), e);
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
