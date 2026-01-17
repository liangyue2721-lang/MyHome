package com.make.stock.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * Stock Consumer Lifecycle Manager
 * Listens to Redis Pub/Sub commands to Pause/Resume Kafka consumers cluster-wide.
 */
@Component
public class StockConsumerLifecycleManager implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(StockConsumerLifecycleManager.class);

    @Resource
    private KafkaListenerEndpointRegistry registry;

    public static final String CHANNEL = "sys:control:channel";
    public static final String CMD_PAUSE = "PAUSE:money-stock-group";
    public static final String CMD_RESUME = "RESUME:money-stock-group";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String cmd = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received system control command: {}", cmd);

        if (CMD_PAUSE.equals(cmd)) {
            pauseConsumers();
        } else if (CMD_RESUME.equals(cmd)) {
            resumeConsumers();
        }
    }

    private void pauseConsumers() {
        try {
            // Pause all containers or specific one if ID is known.
            // Since we didn't assign specific IDs in StockKafkaConsumer, we pause all or filter by group.
            // Assuming we want to pause stock consumption.
            for (MessageListenerContainer container : registry.getListenerContainers()) {
                if (container.getGroupId().equals("money-stock-group")) {
                    if (!container.isPauseRequested()) {
                        container.pause();
                        log.info("Paused container: {}", container.getListenerId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to pause consumers", e);
        }
    }

    private void resumeConsumers() {
        try {
            for (MessageListenerContainer container : registry.getListenerContainers()) {
                if (container.getGroupId().equals("money-stock-group")) {
                    if (container.isPauseRequested()) {
                        container.resume();
                        log.info("Resumed container: {}", container.getListenerId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resume consumers", e);
        }
    }
}
