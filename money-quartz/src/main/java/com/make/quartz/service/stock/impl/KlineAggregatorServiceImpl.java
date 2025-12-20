package com.make.quartz.service.stock.impl;

import com.make.common.utils.ThreadPoolUtil;
import com.make.quartz.service.impl.StockETFrocessor;
import com.make.quartz.service.impl.StockKlineTaskExecutor;
import com.make.quartz.service.impl.StockWatchProcessor;
import com.make.quartz.service.stock.KlineAggregatorService;
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
    private StockETFrocessor stockETFrocessor;

    @Override
    public void runStockKlineTask(int nodeId) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");

        log.info("=====【KlineAggregator】任务开始 NodeId={} TraceId={} =====", nodeId, traceId);

        // 1. 自选股
        CompletableFuture<Void> watchTask = CompletableFuture.runAsync(() -> {
            try {
                stockWatchProcessor.processTask(traceId);
            } catch (Exception e) {
                log.error("[KlineAggregator] WatchTask Error", e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor());

        // 2. ETF
        CompletableFuture<Void> etfTask = CompletableFuture.runAsync(() -> {
            try {
                stockETFrocessor.processTask(traceId);
            } catch (Exception e) {
                log.error("[KlineAggregator] ETFTask Error", e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor());

        // 3. 历史K线
        CompletableFuture<Void> klineTask = CompletableFuture.runAsync(() -> {
            try {
                taskExecutor.executeAll(nodeId, traceId);
            } catch (Exception e) {
                log.error("[KlineAggregator] KlineTask Error", e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor());

        try {
            CompletableFuture.allOf(watchTask, etfTask, klineTask).join();
        } catch (Exception e) {
            log.error("=====【KlineAggregator】任务异常 =====", e);
        }

        log.info("=====【KlineAggregator】任务结束 Cost={} ms =====", System.currentTimeMillis() - start);
    }
}
