package com.make.quartz.util;

import com.make.common.utils.StringUtils;
import com.make.common.utils.ip.IpUtils;
import com.make.common.utils.spring.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;
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

    public NodeRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取当前节点 ID
     *
     * <p>规则：
     * 1. 启动 / 后台线程：使用本机 IP + PID + UUID（不依赖 Servlet）
     * 2. 有 HTTP 请求上下文时：可读取请求 IP（但不会覆盖已有 nodeId）
     */
    public static String getCurrentNodeId() {
        if (CURRENT_NODE_ID != null) {
            return CURRENT_NODE_ID;
        }

        synchronized (NodeRegistry.class) {
            if (CURRENT_NODE_ID != null) {
                return CURRENT_NODE_ID;
            }

            // ===== 1️⃣ 尝试从 HTTP 请求上下文获取（如果存在）=====
            try {
                RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    String ip = IpUtils.getIpAddr();
                    if (StringUtils.isNotEmpty(ip)) {
                        CURRENT_NODE_ID = ip + "-" + UUID.randomUUID();
                        return CURRENT_NODE_ID;
                    }
                }
            } catch (Exception ignore) {
                // 启动阶段必然失败，直接走兜底
            }

            // ===== 2️⃣ 启动阶段 / 后台线程兜底方案 =====
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                String pid = ManagementFactory.getRuntimeMXBean().getName(); // pid@host
                CURRENT_NODE_ID = host + "-" + pid + "-" + UUID.randomUUID();
            } catch (Exception e) {
                CURRENT_NODE_ID = "node-" + UUID.randomUUID();
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
        String nodeId = getCurrentNodeId();

        try {
            redisTemplate.opsForSet().add(NODE_SET_KEY, nodeId);
            redisTemplate.opsForValue().set(
                    NODE_TTL_PREFIX + nodeId,
                    "1",
                    30,
                    TimeUnit.SECONDS
            );
            log.info("[NODE_REGISTER] nodeId={} registered", nodeId);
        } catch (Exception e) {
            log.warn("[NODE_REGISTER_ERR] nodeId={}", nodeId, e);
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
        // 尽量早于 TaskExecutionService
        return Integer.MIN_VALUE;
    }
}
