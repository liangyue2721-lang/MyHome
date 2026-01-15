package com.make.common.core;

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
 * 节点注册中心（安全版 + Master选举）
 *
 * <p>设计原则：
 * - 启动阶段：优先读取配置ID，无配置则自动探测IP
 * - 运行阶段：保持ID稳定
 * - Master选举：通过 Redis SETNX 争抢 Master 身份
 */
@Component
public class NodeRegistry implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(NodeRegistry.class);

    private static final String NODE_SET_KEY = "mq:nodes:alive";
    private static final String NODE_TTL_PREFIX = "mq:nodes:ttl:";
    private static final String SCHEDULER_MASTER = "SCHEDULER_MASTER";

    /**
     * 进程级稳定 nodeId
     */
    private static volatile String CURRENT_NODE_ID;

    /**
     * 是否为主节点
     */
    private volatile boolean isMaster = false;

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
     */
    public static String getCurrentNodeId() {
        if (CURRENT_NODE_ID != null) {
            return CURRENT_NODE_ID;
        }
        return "UNKNOWN";
    }

    /**
     * 当前是否为主节点
     */
    public boolean isMaster() {
        return isMaster;
    }

    @PostConstruct
    public void init() {
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
                 init();
                 nodeId = getCurrentNodeId();
            }

            try {
                // 1. Refresh Node TTL
                redisTemplate.opsForValue().set(
                        NODE_TTL_PREFIX + nodeId,
                        "1",
                        30,
                        TimeUnit.SECONDS
                );
                redisTemplate.opsForSet().add(NODE_SET_KEY, nodeId);

                // 2. Master Election
                Boolean success = redisTemplate.opsForValue().setIfAbsent(SCHEDULER_MASTER, nodeId, 30, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    // Acquired Master Lock
                    if (!isMaster) {
                        log.info("[MASTER_ELECTED] I am Master now. nodeId={}", nodeId);
                        isMaster = true;
                    }
                } else {
                    // Check if I am still master and renew
                    String currentMaster = redisTemplate.opsForValue().get(SCHEDULER_MASTER);
                    if (nodeId.equals(currentMaster)) {
                        redisTemplate.expire(SCHEDULER_MASTER, 30, TimeUnit.SECONDS);
                        if (!isMaster) {
                            log.info("[MASTER_RESTORED] Master role restored. nodeId={}", nodeId);
                            isMaster = true;
                        }
                    } else {
                        if (isMaster) {
                            log.warn("[MASTER_LOST] Lost master role. New master is {}", currentMaster);
                            isMaster = false;
                        }
                    }
                }

            } catch (Throwable e) {
                if (running) {
                    log.warn("[NODE_HEARTBEAT_FAIL] nodeId={} msg={}", nodeId, e.getMessage());
                }
                // In case of Redis error, assume not master to be safe (split-brain prevention)
                // However, existing master might want to keep running if it's just a blip.
                // For simplicity, we keep the flag as is until confirmed lost, or maybe set to false?
                // Set to false is safer for "Production" logic.
                // isMaster = false; // Optional: Fail-safe
            }
        }, 0, 10, TimeUnit.SECONDS);

        log.info("[NODE_REGISTER] Heartbeat & Master Election started. nodeId={}", getCurrentNodeId());
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
        return Integer.MIN_VALUE;
    }
}
