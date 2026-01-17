package com.make.stock.config;

import com.make.stock.mq.StockConsumerLifecycleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import javax.annotation.Resource;

/**
 * Stock Redis Configuration
 * Specifically for subscribing to control channels.
 */
@Configuration
public class StockRedisConfig {

    @Resource
    private StockConsumerLifecycleManager lifecycleManager;

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(lifecycleManager, new PatternTopic(StockConsumerLifecycleManager.CHANNEL));
        return container;
    }
}
