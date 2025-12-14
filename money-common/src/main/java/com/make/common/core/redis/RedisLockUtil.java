package com.make.common.core.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 基于Redis的SETNX命令实现分布式锁，确保在分布式环境下的互斥访问
 */
@Component
public class RedisLockUtil {
    
    private static final Logger log = LoggerFactory.getLogger(RedisLockUtil.class);
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final DefaultRedisScript<String> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();
    
    static {
        // Lua脚本，用于原子性地释放锁
        // 通过比较锁的值来确保只有持有锁的线程才能释放锁
        RELEASE_LOCK_SCRIPT.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end"
        );
        RELEASE_LOCK_SCRIPT.setResultType(String.class);
    }
    
    /**
     * 尝试获取分布式锁
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值，一般使用唯一标识如UUID
     * @param expireTime 过期时间（秒），防止死锁
     * @return true-获取锁成功，false-获取锁失败
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime) {
        try {
            // 使用SET命令的NX和EX选项原子性地设置键和过期时间
            // NX: 只有键不存在时才设置
            // EX: 设置键的过期时间（秒）
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁失败，锁键: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 尝试获取分布式锁（带等待时间）
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值
     * @param expireTime 过期时间（秒）
     * @param timeout 等待超时时间（秒）
     * @return true-获取锁成功，false-获取锁失败
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime, long timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout * 1000;
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                if (tryLock(lockKey, lockValue, expireTime)) {
                    return true;
                }
                // 短暂休眠，避免过度竞争
                Thread.sleep(100);
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁过程中线程被中断，锁键: {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁失败，锁键: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 释放分布式锁
     * 使用Lua脚本确保获取和删除操作的原子性
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值
     * @return true-释放锁成功，false-释放锁失败
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            // 执行Lua脚本，原子性地判断并删除锁
            String result = redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
            return "1".equals(result);
        } catch (Exception e) {
            log.error("释放分布式锁失败，锁键: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 续期分布式锁
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值
     * @param expireTime 新的过期时间（秒）
     * @return true-续期成功，false-续期失败
     */
    public boolean renewLock(String lockKey, String lockValue, long expireTime) {
        try {
            // 使用Lua脚本原子性地续期锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "return redis.call('expire', KEYS[1], ARGV[2]) " +
                           "else return 0 end";
            
            DefaultRedisScript<Long> renewScript = new DefaultRedisScript<>();
            renewScript.setScriptText(script);
            renewScript.setResultType(Long.class);
            
            Long result = redisTemplate.execute(renewScript, 
                                              Collections.singletonList(lockKey), 
                                              lockValue, 
                                              String.valueOf(expireTime));
            return Long.valueOf(1).equals(result);
        } catch (Exception e) {
            log.error("续期分布式锁失败，锁键: {}", lockKey, e);
            return false;
        }
    }
}