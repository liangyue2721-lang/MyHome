package com.make.quartz.service.common;

import com.make.common.core.redis.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 * <p>
 * 对 RedisLockUtil 的轻量级封装，提供更友好的 API，支持 lambda 表达式。
 * </p>
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    @Resource
    private RedisLockUtil redisLockUtil;

    /**
     * 尝试获取锁并执行任务（无返回值）
     *
     * @param lockKey    锁的键
     * @param lockValue  锁的值（通常是 UUID 或 requestId）
     * @param expireTime 锁过期时间（秒）
     * @param waitTime   等待获取锁的时间（秒）
     * @param runnable   要执行的任务
     * @return 是否成功获取锁并执行
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime, long waitTime, Runnable runnable) {
        boolean locked = false;
        try {
            if (waitTime > 0) {
                locked = redisLockUtil.tryLock(lockKey, lockValue, expireTime, waitTime);
            } else {
                locked = redisLockUtil.tryLock(lockKey, lockValue, expireTime);
            }

            if (locked) {
                runnable.run();
                return true;
            } else {
                log.warn("无法获取分布式锁: {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("分布式锁执行异常: {}", lockKey, e);
            throw e;
        } finally {
            if (locked) {
                redisLockUtil.releaseLock(lockKey, lockValue);
            }
        }
    }

    /**
     * 尝试获取锁并执行任务（有返回值）
     *
     * @param lockKey    锁的键
     * @param lockValue  锁的值
     * @param expireTime 锁过期时间（秒）
     * @param waitTime   等待获取锁的时间（秒）
     * @param supplier   要执行的任务
     * @param <T>        返回值类型
     * @return 任务返回值，如果获取锁失败则返回 null
     */
    public <T> T tryLockResult(String lockKey, String lockValue, long expireTime, long waitTime, Supplier<T> supplier) {
        boolean locked = false;
        try {
            if (waitTime > 0) {
                locked = redisLockUtil.tryLock(lockKey, lockValue, expireTime, waitTime);
            } else {
                locked = redisLockUtil.tryLock(lockKey, lockValue, expireTime);
            }

            if (locked) {
                return supplier.get();
            } else {
                log.warn("无法获取分布式锁: {}", lockKey);
                return null;
            }
        } catch (Exception e) {
            log.error("分布式锁执行异常: {}", lockKey, e);
            throw e;
        } finally {
            if (locked) {
                redisLockUtil.releaseLock(lockKey, lockValue);
            }
        }
    }
}
