package com.make.quartz.config;

import com.make.quartz.service.impl.IRealTimeStockServiceImpl;
import org.quartz.impl.jdbcjobstore.Semaphore;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * Quartz 分布式锁实现（基于 Redis）
 *
 * <p>
 * 该类实现 Quartz 的 Semaphore 接口，用于替换默认数据库锁。
 * 通过 Redisson 提供的 RLock 分布布式锁，避免数据库锁竞争，提高调度性能。
 * </p>
 *
 * <p>
 * 功能特点：
 * 1. 基于 Redis 的分布式锁，支持多节点 Quartz 集群调度。
 * 2. 使用 Redisson 提供的自动续期机制，避免任务长时间执行导致锁提前过期。
 * 3. 与 Quartz 原有接口兼容，只修改锁逻辑，不影响 JobStore 数据存储。
 * </p>
 * <p>
 * 使用方式：
 * 在 Quartz 配置中替换默认的 DB LockHandler，注入该实现。
 *
 * @author
 */
@Component
public class RedisQuartzSemaphore implements Semaphore {

    /**
     * 日志记录器，用于记录服务执行过程中的日志信息
     */
    private static final Logger log = LoggerFactory.getLogger(RedisQuartzSemaphore.class);

    /**
     * Redisson 客户端，用于操作 Redis 分布式锁
     */
    private static RedissonClient redissonClient;

    /**
     * 无参构造函数，供Quartz通过反射创建实例时使用
     */
    public RedisQuartzSemaphore() {
    }

    /**
     * 获取 Redisson RLock 对象（内部使用 holder 获取 client）
     */
    public RLock getLock(String key) {
        RedissonClient client = RedissonClientHolder.getClient(); // 可能抛 IllegalStateException，调用方要能容忍/记录
        return client.getLock(key);
    }

    /**
     * 尝试获取锁（Quartz 调用）
     *
     * @param conn     JDBC 连接（该实现不使用）
     * @param lockName 锁名称（Quartz 提供）
     * @return 成功返回 true，失败或异常返回 false
     */
    @Override
    public boolean obtainLock(Connection conn, String lockName) {
        String redisKey = "quartz:lock:" + lockName;
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        String currentTime = getCurrentTime();

        log.info("🔐 [{}] 尝试获取分布式锁 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {}",
                currentTime, lockName, threadName, threadId, redisKey);

        RLock lock = null;
        try {
            lock = getLock(redisKey);
        } catch (IllegalStateException e) {
            log.error("❌ [{}] RedissonClient 未就绪，无法获取锁 | Redis键: {} | 错误: {}",
                    currentTime, redisKey, e.getMessage());
            return false;
        }

        int retryCount = 0;
        final int maxRetries = 3;
        final long retryDelay = 1000; // 1秒重试间隔
        
        while (retryCount <= maxRetries) {
            try {
                // 使用 Redisson 的 watchdog 自动续期机制
                // 等待最多 5 秒去获取锁
                boolean acquired = lock.tryLock(5, TimeUnit.SECONDS);
                
                if (acquired) {
                    log.info("✅ [{}] 成功获取分布式锁 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {} | 重试次数: {}",
                            currentTime, lockName, threadName, threadId, redisKey, retryCount);
                    return true;
                }
                
                if (retryCount < maxRetries) {
                    log.warn("⏰ [{}] 获取分布式锁超时，准备第 {} 次重试 | 锁名称: {} | Redis键: {}",
                        currentTime, retryCount + 1, lockName, redisKey);
                        
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryDelay * (retryCount + 1)); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("⛔ [{}] 获取锁重试等待时被中断 | Redis键: {}",
                                getCurrentTime(), redisKey);
                        return false;
                    }
                } else {
                    log.warn("⏰ [{}] 获取分布式锁超时，已达到最大重试次数({}) | 锁名称: {} | Redis键: {}",
                            currentTime, maxRetries, lockName, redisKey);
                    return false;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("⛔ [{}] 获取分布式锁被中断 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {} | 错误: {}",
                        currentTime, lockName, threadName, threadId, redisKey, ie.getMessage(), ie);
                return false;
            } catch (Exception e) {
                log.error("💥 [{}] 获取分布式锁异常 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {} | 错误: {}",
                        currentTime, lockName, threadName, threadId, redisKey, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    log.warn("🔄 [{}] 准备第 {} 次重试（异常后） | Redis键: {}", 
                            getCurrentTime(), retryCount + 1, redisKey);
                            
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryDelay * (retryCount + 1)); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("⛔ [{}] 获取锁重试等待时被中断 | Redis键: {}",
                                getCurrentTime(), redisKey);
                        return false;
                    }
                } else {
                    return false;
                }
            }
            retryCount++;
        }
        
        return false;
    }

    /**
     * 释放锁（Quartz 调用）
     *
     * @param lockName 锁名称
     */
    @Override
    public void releaseLock(String lockName) {
        String redisKey = "quartz:lock:" + lockName;
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        String currentTime = getCurrentTime();

        RLock lock;
        try {
            lock = getLock(redisKey);
        } catch (IllegalStateException e) {
            log.warn("⚠️ [{}] RedissonClient 未就绪，无法释放锁，直接返回 | Redis键: {} | 错误: {}",
                    currentTime, redisKey, e.getMessage());
            return;
        }

        try {
            if (lock.isLocked()) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("🔓 [{}] 成功释放分布式锁 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {}",
                            currentTime, lockName, threadName, threadId, redisKey);
                } else {
                    // 可能锁已经被其他线程释放或超时
                    log.warn("⚠️ [{}] 尝试释放锁但当前线程未持有 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {}",
                            currentTime, lockName, threadName, threadId, redisKey);
                }
            } else {
                log.warn("⚠️ [{}] 尝试释放已释放的锁 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {}",
                        currentTime, lockName, threadName, threadId, redisKey);
            }
        } catch (IllegalMonitorStateException imse) {
            // unlock 可能抛出该异常（例如锁已过期或不是当前线程持有）
            log.warn("⚠️ [{}] 释放分布式锁时发生 IllegalMonitorStateException | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {} | 错误: {}",
                    currentTime, lockName, threadName, threadId, redisKey, imse.getMessage());
        } catch (Exception e) {
            log.error("💥 [{}] 释放分布式锁异常 | 锁名称: {} | 线程: {}(ID:{}) | Redis键: {} | 错误: {}",
                    currentTime, lockName, threadName, threadId, redisKey, e.getMessage(), e);
        }
    }

    /**
     * 指明此 lockHandler 不依赖 JDBC 连接
     */
    @Override
    public boolean requiresConnection() {
        return false;
    }

    /**
     * 获取当前时间戳（用于日志记录）
     * @return 格式化的时间字符串
     */
    private String getCurrentTime() {
        return LocalTime.now().toString();
    }
}
