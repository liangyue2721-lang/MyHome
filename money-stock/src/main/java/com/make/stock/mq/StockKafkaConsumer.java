package com.make.stock.mq;

import com.make.common.constant.KafkaTopics;
import com.make.stock.service.scheduled.IRealTimeStockService;
import com.make.stock.service.scheduled.stock.KlineAggregatorService;
import com.make.stock.service.scheduled.stock.ProfitService;
import com.make.stock.service.scheduled.stock.StockInfoService;
import com.make.stock.service.scheduled.stock.WatchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;

/**
 * Stock Kafka Consumer
 * Handles scheduled stock tasks via Kafka topics.
 */
@Component
public class StockKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockKafkaConsumer.class);

    @Resource
    private StockInfoService stockInfoService;

    @Resource
    private WatchService watchService;

    @Resource
    private ProfitService profitService;

    @Resource
    private KlineAggregatorService klineAggregatorService;

    @Resource
    private IRealTimeStockService realTimeStockService; // For legacy methods if any

    @KafkaListener(topics = KafkaTopics.TOPIC_WATCH_STOCK_US, groupId = "money-stock-group")
    public void updateWatchStockUs(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_WATCH_STOCK_US]");
        watchService.updateWatchStockUs();
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_QUERY_LISTING, groupId = "money-stock-group")
    public void queryListingStatusColumn(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_QUERY_LISTING]");
        // Param date is usually today/midnight.
        // RealTimeTask logic: queryListingStatusColumn(midnight)
        // We can calculate midnight here or pass in payload.
        // For simplicity, recalculate midnight as Quartz did.
        Date midnight = getDateMidnight();
        stockInfoService.queryListingStatusColumn(midnight);
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_NEW_STOCK_INFO, groupId = "money-stock-group")
    public void refreshNewStockInformation(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_NEW_STOCK_INFO]");
        stockInfoService.refreshNewStockInformation();
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_STOCK_PROFIT_UPDATE, groupId = "money-stock-group")
    public void updateStockProfitData(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_STOCK_PROFIT_UPDATE]");
        try {
            profitService.updateStockProfitData();
        } catch (IOException e) {
            log.error("Failed to update stock profit data", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_STOCK_PROFIT_QUERY, groupId = "money-stock-group")
    public void queryStockProfitData(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_STOCK_PROFIT_QUERY]");
        profitService.queryStockProfitData();
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_ETF_UPDATE, groupId = "money-stock-group")
    public void updateEtfData(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_ETF_UPDATE]");
        try {
            stockInfoService.updateEtfData();
        } catch (IOException e) {
             log.error("Failed to update ETF data", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_WATCH_STOCK_PROFIT, groupId = "money-stock-group")
    public void updateWatchStockProfitData(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_WATCH_STOCK_PROFIT]");
        watchService.updateWatchStockProfitData();
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_ARCHIVE_DAILY_STOCK, groupId = "money-stock-group")
    public void archiveDailyStockData(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_ARCHIVE_DAILY_STOCK]");
        profitService.archiveDailyStockData();
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_WATCH_STOCK_YEAR_LOW, groupId = "money-stock-group")
    public void updateWatchStockYearLow(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_WATCH_STOCK_YEAR_LOW]");
        watchService.updateWatchStockYearLow();
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_STOCK_PRICE_TASK, groupId = "money-stock-group")
    public void updateStockPriceTaskRunning(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_STOCK_PRICE_TASK]");
        // Quartz passed nodeId. Here we can use a default or parse from payload if needed.
        // runStockKlineTask(nodeId) logic uses nodeId to identify who is running.
        // We can pass 0 or 1, or parse from payload if Quartz sent it.
        // Assuming payload might contain nodeId, but for now safely invoke.
        // Actually runStockKlineTask seems to take int nodeId.
        klineAggregatorService.runStockKlineTask(1);
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_STOCK_REFRESH, groupId = "money-stock-group")
    public void stockRefresh(ConsumerRecord<String, String> record) {
        log.info("Consume [TOPIC_STOCK_REFRESH]");
        // Logic from RealTimeTask.updateInMemoryData (commented out in original but maybe needed?)
        // Or logic that was in IRealTimeStockServiceImpl
        // realTimeStockService.refreshInMemoryMapEntries();
        // realTimeStockService.batchSyncStockDataToDB2();

        // Check if we should uncomment this based on migration goals.
        // RealTimeTask.java had it commented out with "Logic migrated to IStockTaskServiceImpl... scheduled by Quartz".
        // If it was already migrated, maybe we don't need to call it here?
        // But if Quartz was scheduling "StockKlineTaskExecutor", then that's different.

        // If the user wants "Quartz -> Kafka -> Business", and "All original Quartz tasks",
        // we should map whatever was active in Quartz.
        // If RealTimeTask.updateInMemoryData was commented out, we don't map it.
    }

    private Date getDateMidnight() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
