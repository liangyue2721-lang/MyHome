package com.make.framework.config;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.ProducerListener;

/**
 * Kafka Producer Configuration for Logging
 */
@Configuration
public class KafkaProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Bean
    public ProducerListener<String, Object> producerListener() {
        return new ProducerListener<String, Object>() {
            @Override
            public void onSuccess(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata) {
                log.info("[PRODUCE_SUCCESS] Topic={}, Key={}, Offset={}",
                        producerRecord.topic(), producerRecord.key(), recordMetadata.offset());
            }

            @Override
            public void onError(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata, Exception exception) {
                log.error("[PRODUCE_ERROR] Topic={}, Key={}, Error={}",
                        producerRecord.topic(), producerRecord.key(), exception.getMessage());
            }
        };
    }
}
