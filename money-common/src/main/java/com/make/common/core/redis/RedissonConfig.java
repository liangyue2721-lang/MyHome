package com.make.common.core.redis;

import org.springframework.beans.factory.annotation.Value;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 *
 * <p>
 * 该类用于配置 RedissonClient 对象，提供分布式锁、分布式集合等高级功能。
 * Quartz 集群模式下，使用 Redisson 分布式锁代替数据库锁，提升高并发性能。
 * </p>
 *
 * @author
 */
@Configuration
public class RedissonConfig {

    /**
     * Redis 服务器主机地址
     */
    @Value("${spring.redis.host}")
    private String redisHost;

    /**
     * Redis 服务器端口号
     */
    @Value("${spring.redis.port}")
    private int redisPort;

    /**
     * Redis 服务器密码
     */
    @Value("${spring.redis.password}")
    private String redisPassword;
    /**
     * 创建 Redisson 客户端 Bean
     *
     * @return RedissonClient 实例，用于分布式锁和其他分布式对象操作
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // 创建 Redisson 配置对象
        Config config = new Config();

        // 使用单节点模式配置 Redis 地址、密码和数据库索引
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort) // 设置 Redis 服务器地址和端口
                .setPassword(redisPassword) // 设置 Redis 服务器密码
                .setTimeout(50000)
                .setDatabase(0); // 设置使用的数据库索引，默认为 0

        // 创建并返回 RedissonClient 实例
        return Redisson.create(config);
    }
}