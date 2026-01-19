package com.make.framework.config;

import com.make.common.utils.ip.IpUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Kafka Configuration for Reliability
 * - Global Error Handling
 * - Dead Letter Queue (DLQ) Strategy
 * - Custom Consumer Factory with IP-based Client ID
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    /**
     * Custom ConsumerFactory to append Node IP to Client ID.
     * This helps identify the consumer node in Kafka Monitor (Consumer Groups)
     * even if the Broker sees 127.0.0.1 as the Host.
     */
    @Bean
    public ConsumerFactory<String, String> kafkaConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = properties.buildConsumerProperties();

        String ip = IpUtils.getHostIp();
        String clientId = (String) config.get(ConsumerConfig.CLIENT_ID_CONFIG);
        if (clientId == null) {
            clientId = "consumer";
        }
        // Append IP to Client ID (e.g., money-group-192.168.1.5)
        // Spring Kafka will further append -n (e.g., -1, -2) for concurrency.
        String newClientId = clientId + "-" + ip;
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, newClientId);

        log.info("Configured Kafka Client ID with IP: {}", newClientId);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Default Error Handler with DLQ Support
     * - Retries 3 times with 1 second interval.
     * - If exhausted, sends to DLQ topic ({original_topic}.DLQ).
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // Dead Letter Recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (r, e) -> {
                    log.error("Message failed after retries. Sending to DLQ. Topic: {}, Key: {}, Error: {}",
                            r.topic(), r.key(), e.getMessage());
                    return new TopicPartition(r.topic() + ".DLQ", r.partition());
                });

        // Fixed BackOff: 1000ms, 3 attempts
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Ensure that we don't infinitely retry for irrecoverable errors (optional, usually DefaultErrorHandler handles checking)
        // handler.addNotRetryableExceptions(IllegalArgumentException.class); // Example

        return handler;
    }
}
