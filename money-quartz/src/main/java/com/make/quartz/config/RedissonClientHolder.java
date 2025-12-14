package com.make.quartz.config;


import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RedissonClientHolder
 *
 * 说明：
 * - 这个 Holder 由 Spring 注入 RedissonClient，并提供静态访问接口，
 *   以便 Quartz 通过反射创建的非 Spring 管理实例也能安全地获取 RedissonClient。
 * - 线程安全：使用 volatile 保证可见性，构造完成后即可读取。
 */
@Component
public class RedissonClientHolder {

    private static volatile RedissonClient client;

    @Autowired
    public RedissonClientHolder(RedissonClient redissonClient) {
        RedissonClientHolder.client = redissonClient;
    }

    /**
     * 获取 RedissonClient 实例（静态方法，供非 Spring 实例调用）
     *
     * @return RedissonClient（如果尚未注入将抛出 IllegalStateException）
     */
    public static RedissonClient getClient() {
        if (client == null) {
            throw new IllegalStateException("RedissonClient 尚未注入（Spring 容器可能未准备好）");
        }
        return client;
    }

    /**
     * （可选）用于测试或热替换 RedissonClient
     */
    public static void setClient(RedissonClient redissonClient) {
        RedissonClientHolder.client = redissonClient;
    }
}
