package com.make.stock.mq;

import com.alibaba.fastjson2.JSON;
import com.make.common.constant.KafkaTopics;
import com.make.common.utils.ip.IpUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.domain.StockRefreshTask;
import com.make.stock.service.scheduled.IRealTimeStockService;
import com.make.stock.service.scheduled.impl.StockWatchProcessor;
import com.make.stock.service.scheduled.stock.KlineAggregatorService;
import com.make.stock.service.scheduled.stock.ProfitService;
import com.make.stock.service.scheduled.stock.StockInfoService;
import com.make.stock.service.scheduled.stock.WatchService;
import com.make.stock.service.scheduled.stock.handler.IStockRefreshHandler;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
    private StockTaskQueueService queueService;

    @Resource
    private IStockRefreshHandler stockRefreshHandler;

    @Resource
    private StockWatchProcessor stockWatchProcessor;

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

    @KafkaListener(topics = KafkaTopics.TOPIC_STOCK_REFRESH, groupId = "money-stock-group", containerFactory = "stockBatchFactory", properties = {"max.poll.records=10"})
    public void stockRefresh(List<ConsumerRecord<String, String>> records) {
        log.info("Consume [TOPIC_STOCK_REFRESH] batch size: {}", records.size());
        String currentNodeId = IpUtils.getHostIp();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            String json = record.value();

            try {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        StockRefreshTask task = JSON.parseObject(json, StockRefreshTask.class);
                        if (task == null) return;

                        // Try lock
                        if (queueService.tryLockStock(task.getStockCode(), currentNodeId)) {
                            try {
                                stockRefreshHandler.refreshStock(task);
                            } finally {
                                queueService.releaseLock(task.getStockCode(), currentNodeId);
                            }
                        } else {
                            log.debug("Skipping stock {} (locked by another node)", task.getStockCode());
                        }

                        // Always delete status (whether executed or skipped) to prevent zombie tasks in monitoring
                        queueService.deleteStatus(task.getStockCode(), task.getTraceId());

                        // Decrement batch regardless of lock acquisition (skipped tasks count as processed)
                        long remaining = queueService.decrementBatch(task.getTraceId());
                        if (remaining == 0) {
                            log.info("Batch completed (traceId={}). Triggering next batch...", task.getTraceId());
                            stockWatchProcessor.triggerNextBatch();
                        } else if (remaining < 0) {
                             if (queueService.tryLockRecovery(task.getTraceId())) {
                                 log.info("Recovery lock acquired. Triggering next batch... traceId={}", task.getTraceId());
                                 stockWatchProcessor.triggerNextBatch();
                             }
                        }
                    } catch (Exception e) {
                        log.error("Failed to process stock refresh task: {}", json, e);
                    }
                }, ThreadPoolUtil.getWatchStockExecutor());

                futures.add(future);
            } catch (Exception e) {
                log.error("Failed to submit task to executor (skipping in this batch): {}", json, e);
            }
        }

        // Wait for all tasks in this batch to complete before acknowledging
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
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
