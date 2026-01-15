package com.make.finance.mq;

import com.alibaba.fastjson2.JSON;
import com.make.common.constant.KafkaTopics;
import com.make.common.annotation.IdempotentConsumer;
import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.finance.service.scheduled.finance.CreditCardService;
import com.make.finance.service.scheduled.finance.DepositService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Finance Kafka Consumer
 */
@Component
public class FinanceTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(FinanceTaskConsumer.class);

    @Resource
    private FinanceTaskHandler financeTaskHandler;

    @Resource
    private CreditCardService creditCardService;

    @KafkaListener(topics = KafkaTopics.TOPIC_DEPOSIT_UPDATE, groupId = "money-finance-group")
    public void updateDepositAmount(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_DEPOSIT_UPDATE] key={}", record.key());
        financeTaskHandler.handleDepositUpdate(record.key());
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_ICBC_DEPOSIT_UPDATE, groupId = "money-finance-group")
    public void updateICBCDepositAmount(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_ICBC_DEPOSIT_UPDATE] key={}", record.key());
        financeTaskHandler.handleICBCDepositUpdate(record.key());
    }

}
