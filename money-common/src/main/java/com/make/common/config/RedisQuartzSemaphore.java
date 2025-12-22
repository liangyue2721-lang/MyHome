package com.make.common.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 分布式信号量（用于 Quartz / 定时任务互斥执行）
 * <p>
 * 设计原则：
 * 1. 多实例安全
 * 2. 任何异常情况下都不会造成“永久占锁”
 * 3. 锁必须有 TTL
 * 4. 只能由 owner 释放
 */
public class RedisQuartzSemaphore {

    /**
     * Redis Key 前缀
     */
    private static final String KEY_PREFIX = "quartz:semaphore:";

    /**
     * 默认锁过期时间（秒）
     * 必须 > 单次任务最大执行时间
     */
    private static final long DEFAULT_EXPIRE_SECONDS = 300;

    /**
     * 当前实例唯一标识
     */
    private static final String INSTANCE_ID = UUID.randomUUID().toString();

    private static StringRedisTemplate redisTemplate;

    /**
     * Lua 脚本：仅当 value == owner 时才删除 key
     */
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) " +
                            "else return 0 end",
                    Long.class
            );

    /**
     * 注入 RedisTemplate
     */
    public static void init(StringRedisTemplate template) {
        redisTemplate = template;
    }

    /**
     * 尝试获取任务执行信号量
     *
     * @param jobKey 任务唯一标识（建议 jobId）
     * @return true = 获取成功，可以执行；false = 其他实例正在执行
     */
    public static boolean tryAcquire(String jobKey) {
        String key = buildKey(jobKey);

        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                key,
                INSTANCE_ID,
                Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS)
        );

        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放信号量
     * <p>
     * 只有持有锁的实例才能释放，防止误删其他实例的执行锁
     *
     * @param jobKey 任务唯一标识
     */
    public static void release(String jobKey) {
        String key = buildKey(jobKey);

        try {
            redisTemplate.execute(
                    RELEASE_SCRIPT,
                    Collections.singletonList(key),
                    INSTANCE_ID
            );
        } catch (Exception e) {
            // 释放失败不抛异常，避免影响主流程
            // 建议这里加监控
        }
    }

    /**
     * 续期信号量（可选，用于超长任务）
     *
     * @param jobKey 任务唯一标识
     * @return true = 续期成功；false = 当前实例已不持有锁
     */
    public static boolean renew(String jobKey) {
        String key = buildKey(jobKey);

        String owner = redisTemplate.opsForValue().get(key);
        if (!INSTANCE_ID.equals(owner)) {
            return false;
        }

        redisTemplate.expire(key, Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS));
        return true;
    }

    /**
     * 构建 Redis key
     */
    private static String buildKey(String jobKey) {
        return KEY_PREFIX + jobKey;
    }
}

