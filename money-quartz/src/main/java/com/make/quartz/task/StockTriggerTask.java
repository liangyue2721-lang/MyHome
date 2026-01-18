package com.make.quartz.task;

import com.make.common.constant.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 股票业务触发任务
 * 仅负责向 Kafka 发送触发消息，不包含业务逻辑
 */
@Component("stockTriggerTask")
public class StockTriggerTask {

    private static final Logger log = LoggerFactory.getLogger(StockTriggerTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 触发：刷新美股关注列表
     */
    public void triggerWatchStockUs() {
        send(KafkaTopics.TOPIC_WATCH_STOCK_US, "trigger");
    }

    /**
     * 触发：查询股票上市状态
     */
    public void triggerQueryListing() {
        send(KafkaTopics.TOPIC_QUERY_LISTING, "trigger");
    }

    /**
     * 触发：刷新新股信息
     */
    public void triggerNewStockInfo() {
        send(KafkaTopics.TOPIC_NEW_STOCK_INFO, "trigger");
    }

    /**
     * 触发：更新股票盈利数据
     */
    public void triggerStockProfitUpdate() {
        send(KafkaTopics.TOPIC_STOCK_PROFIT_UPDATE, "trigger");
    }

    /**
     * 触发：查询股票盈利数据
     */
    public void triggerStockProfitQuery() {
        send(KafkaTopics.TOPIC_STOCK_PROFIT_QUERY, "trigger");
    }

    /**
     * 触发：更新ETF数据
     */
    public void triggerEtfUpdate() {
        send(KafkaTopics.TOPIC_ETF_UPDATE, "trigger");
    }

    /**
     * 触发：更新关注股票盈利数据
     */
    public void triggerWatchStockProfit() {
        send(KafkaTopics.TOPIC_WATCH_STOCK_PROFIT, "trigger");
    }

    /**
     * 触发：每日股票数据归档
     */
    public void triggerArchiveDailyStock() {
        send(KafkaTopics.TOPIC_ARCHIVE_DAILY_STOCK, "trigger");
    }

    /**
     * 触发：更新关注股票年度低点
     */
    public void triggerWatchStockYearLow() {
        send(KafkaTopics.TOPIC_WATCH_STOCK_YEAR_LOW, "trigger");
    }

    /**
     * 触发：股票价格刷新任务（Kline/Batch）
     */
    public void triggerStockPriceTask() {
        // 可以传递 traceId 或其他上下文
        String traceId = UUID.randomUUID().toString();
        send(KafkaTopics.TOPIC_STOCK_PRICE_TASK, traceId);
    }

    private void send(String topic, String payload) {
        String key = UUID.randomUUID().toString();
        log.info("Triggering Kafka topic: {} with key: {}", topic, key);
        kafkaTemplate.send(topic, key, payload);
    }
}
