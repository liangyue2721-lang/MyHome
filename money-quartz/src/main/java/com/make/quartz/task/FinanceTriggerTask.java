package com.make.quartz.task;

import com.make.common.constant.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 财务业务触发任务
 * 仅负责向 Kafka 发送触发消息，不包含业务逻辑
 */
@Component("financeTriggerTask")
public class FinanceTriggerTask {

    private static final Logger log = LoggerFactory.getLogger(FinanceTriggerTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 触发：更新存款金额
     */
    public void triggerDepositUpdate() {
        send(KafkaTopics.TOPIC_DEPOSIT_UPDATE, "trigger");
    }

    /**
     * 触发：更新工行存款
     */
    public void triggerIcbcDepositUpdate() {
        send(KafkaTopics.TOPIC_ICBC_DEPOSIT_UPDATE, "trigger");
    }

    /**
     * 触发：建行信用卡账单处理
     */
    public void triggerCcbCreditCard() {
        send(KafkaTopics.TOPIC_CCB_CREDIT_CARD, "trigger");
    }

    private void send(String topic, String payload) {
        String key = UUID.randomUUID().toString();
        log.info("Triggering Kafka topic: {} with key: {}", topic, key);
        kafkaTemplate.send(topic, key, payload);
    }
}
