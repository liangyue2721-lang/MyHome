package com.make.system.mq;

import com.make.common.constant.KafkaTopics;
import com.make.common.annotation.IdempotentConsumer;
import com.make.system.executor.DatabaseBackupExecutor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * System Kafka Consumer
 */
@Component
public class SystemTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(SystemTaskConsumer.class);

    @Resource
    private DatabaseBackupExecutor databaseBackupExecutor;

    @KafkaListener(topics = KafkaTopics.TOPIC_SYSTEM_BACKUP, groupId = "money-system-group")
    public void executeBackup(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_SYSTEM_BACKUP] key={}", record.key());
        try {
            handleBackup(record.key());
        } catch (Exception e) {
            log.error("Database backup failed", e);
        }
    }

    @IdempotentConsumer(key = "#traceId")
    public void handleBackup(String traceId) throws Exception {
        databaseBackupExecutor.executeBackup();
    }
}
