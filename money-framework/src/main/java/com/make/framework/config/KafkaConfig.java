package com.make.framework.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Configuration for Reliability
 * - Global Error Handling
 * - Dead Letter Queue (DLQ) Strategy
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

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
