package com.make.quartz.util;

import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 节点注册与心跳管理器
 * 负责节点注册、心跳更新、负载上报等功能
 */
@Component
public class NodeRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(NodeRegistry.class);
    
    /**
     * Redis key定义
     */
    private static final String SCHEDULER_NODES_KEY = "SCHEDULER_NODES"; // 所有节点集合
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:"; // 节点信息前缀
    private static final String SCHEDULER_NODE_USAGE_SUFFIX = ":USAGE"; // 节点使用率后缀
    private static final String SCHEDULER_NODE_HEARTBEAT_SUFFIX = ":HEARTBEAT"; // 节点心跳后缀
    
    /**
     * 当前节点ID，格式为IP:UUID
     */
    private static final String CURRENT_NODE_ID = IpUtils.getHostIp() + ":" + UUID.randomUUID().toString();
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private ScheduledExecutorService heartbeatScheduler;
    
    // 标记应用是否正在关闭
    private volatile boolean shuttingDown = false;
    
    /**
     * 初始化节点注册与心跳任务
     */
    @PostConstruct
    public void init() {
        // 注册当前节点
        registerNode();
        
        // 启动心跳任务
        startHeartbeat();
        
        log.info("节点注册器初始化完成，当前节点ID: {}", CURRENT_NODE_ID);
    }
    
    /**
     * 注册当前节点到Redis
     */
    private void registerNode() {
        try {
            // 检查应用是否正在关闭
            if (shuttingDown) {
                log.debug("应用正在关闭，跳过节点注册");
                return;
            }
            
            // 将当前节点添加到节点集合中
            redisTemplate.opsForSet().add(SCHEDULER_NODES_KEY, CURRENT_NODE_ID);
            
            // 设置过期时间，防止节点宕机后信息残留
            redisTemplate.expire(SCHEDULER_NODES_KEY, 300, TimeUnit.SECONDS);
            
            log.info("节点注册成功: {}", CURRENT_NODE_ID);
        } catch (IllegalStateException e) {
            // Redis连接工厂被销毁
            log.warn("节点注册失败，Redis连接不可用: {}", CURRENT_NODE_ID);
        } catch (Exception e) {
            log.error("节点注册失败: {}", CURRENT_NODE_ID, e);
        }
    }
    
    /**
     * 启动心跳任务
     */
    private void startHeartbeat() {
        // 创建单线程调度器用于心跳任务
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "NodeHeartbeat-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        
        // 每30秒更新一次心跳和负载信息
        heartbeatScheduler.scheduleAtFixedRate(this::updateHeartbeat, 0, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 更新心跳和负载信息
     */
    private void updateHeartbeat() {
        try {
            // 检查应用是否正在关闭
            if (shuttingDown) {
                log.debug("应用正在关闭，跳过心跳更新");
                return;
            }
            
            // 更新心跳时间戳
            String heartbeatKey = SCHEDULER_NODE_PREFIX + CURRENT_NODE_ID + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
            redisTemplate.opsForValue().set(heartbeatKey, String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(heartbeatKey, 300, TimeUnit.SECONDS); // 5分钟过期
            
            // 更新节点负载信息
            double usage = calculateNodeUsage();
            String usageKey = SCHEDULER_NODE_PREFIX + CURRENT_NODE_ID + SCHEDULER_NODE_USAGE_SUFFIX;
            redisTemplate.opsForValue().set(usageKey, String.valueOf(usage));
            redisTemplate.expire(usageKey, 300, TimeUnit.SECONDS); // 5分钟过期
            
            log.debug("节点心跳更新成功: {}，负载: {}", CURRENT_NODE_ID, usage);
        } catch (IllegalStateException e) {
            // Redis连接工厂被销毁
            log.warn("更新节点心跳失败，Redis连接不可用: {}", CURRENT_NODE_ID);
        } catch (Exception e) {
            log.error("更新节点心跳失败: {}", CURRENT_NODE_ID, e);
        }
    }
    
    /**
     * 计算节点负载使用率
     * 仅考虑内存使用率
     * 
     * @return 负载使用率，范围0-1
     */
    private double calculateNodeUsage() {
        try {
            // 获取内存使用率
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) heapUsed / heapMax;
            
            // 仅使用内存使用率作为节点负载指标
            return memoryUsage;
        } catch (Exception e) {
            log.warn("计算节点负载时发生异常，使用默认负载值", e);
            return 0.5; // 默认50%负载
        }
    }
    
    /**
     * 获取当前节点ID
     * 
     * @return 当前节点ID
     */
    public static String getCurrentNodeId() {
        return CURRENT_NODE_ID;
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        // 标记应用正在关闭
        shuttingDown = true;
        
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            try {
                if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 从Redis中移除当前节点信息
        try {
            if (!shuttingDown) { // 再次检查，确保Redis连接仍然可用
                redisTemplate.opsForSet().remove(SCHEDULER_NODES_KEY, CURRENT_NODE_ID);
                String usageKey = SCHEDULER_NODE_PREFIX + CURRENT_NODE_ID + SCHEDULER_NODE_USAGE_SUFFIX;
                redisTemplate.delete(usageKey);
                String heartbeatKey = SCHEDULER_NODE_PREFIX + CURRENT_NODE_ID + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                redisTemplate.delete(heartbeatKey);
            }
        } catch (IllegalStateException e) {
            // Redis连接工厂被销毁，这是预期的，因为应用正在关闭
            log.debug("清理节点信息时Redis连接不可用，这是正常的关闭过程: {}", CURRENT_NODE_ID);
        } catch (Exception e) {
            log.error("清理节点信息失败: {}", CURRENT_NODE_ID, e);
        }
        
        log.info("节点注册器资源清理完成");
    }
}