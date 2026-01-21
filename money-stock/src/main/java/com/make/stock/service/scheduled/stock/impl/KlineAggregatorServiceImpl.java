package com.make.stock.service.scheduled.stock.impl;

import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.service.scheduled.impl.StockETFProcessor;
import com.make.stock.service.scheduled.impl.StockKlineTaskExecutor;
import com.make.stock.service.scheduled.impl.StockWatchProcessor;
import com.make.stock.service.scheduled.stock.KlineAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * K线聚合服务实现类
 * 迁移自 IStockTaskServiceImpl
 */
@Service
public class KlineAggregatorServiceImpl implements KlineAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(KlineAggregatorServiceImpl.class);

    @Autowired
    private StockKlineTaskExecutor taskExecutor;

    @Resource
    private StockWatchProcessor stockWatchProcessor;

    @Resource
    private StockETFProcessor stockETFProcessor;

    @Override
    public void runStockKlineTask(int nodeId) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");

        log.info("=====【KlineAggregator】任务开始 NodeId={} TraceId={} =====", nodeId, traceId);

        // 1. 自选股
        CompletableFuture<Void> watchTask = CompletableFuture.runAsync(() -> {
            try {
                stockWatchProcessor.runWatchdog();
            } catch (Exception e) {
                log.error("[KlineAggregator] WatchTask Error | TraceId: {}", traceId, e);
                throw new RuntimeException(e); // Propagate exception to future
            }
        }, ThreadPoolUtil.getWatchStockExecutor()).exceptionally(e -> {
             log.error("[KlineAggregator] WatchTask Failed | TraceId: {}", traceId, e);
             return null;
        });

        // 2. ETF
        // 重构说明：生产 Kafka 消息，交由消费者集群处理
        CompletableFuture<Void> etfTask = CompletableFuture.runAsync(() -> {
            try {
                // 提交任务到 Kafka (忽略 NodeId)
                stockETFProcessor.submitTasks(traceId);
            } catch (Exception e) {
                log.error("[KlineAggregator] ETFTask Error | TraceId: {}", traceId, e);
                throw new RuntimeException(e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor()).exceptionally(e -> {
            log.error("[KlineAggregator] ETFTask Failed | TraceId: {}", traceId, e);
            return null;
        });

        // 3. 历史K线
        // 重构说明：生产 Kafka 消息，交由消费者集群处理
        CompletableFuture<Void> klineTask = CompletableFuture.runAsync(() -> {
            try {
                // 提交任务到 Kafka (忽略 NodeId)
                taskExecutor.submitTasks(traceId);
            } catch (Exception e) {
                log.error("[KlineAggregator] KlineTask Error | TraceId: {}", traceId, e);
                throw new RuntimeException(e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor()).exceptionally(e -> {
            log.error("[KlineAggregator] KlineTask Failed | TraceId: {}", traceId, e);
            return null;
        });

        try {
            // join() will throw CompletionException if any future failed (and wasn't handled by exceptionally,
            // but we added exceptionally which returns null, so join() will succeed but log errors)
            // If we want the main task to reflect partial success, this is fine.
//            CompletableFuture.allOf(watchTask, etfTask, klineTask).join();
        } catch (Exception e) {
            log.error("=====【KlineAggregator】任务聚合异常 | TraceId: {} =====", traceId, e);
        }

        log.info("=====【KlineAggregator】任务结束 Cost={} ms =====", System.currentTimeMillis() - start);
    }
}
