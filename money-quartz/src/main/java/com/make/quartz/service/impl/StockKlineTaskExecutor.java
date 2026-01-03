package com.make.quartz.service.impl;

import com.google.common.collect.Lists;
import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.StockKlineTask;
import com.make.stock.domain.dto.ProcessResult;
import com.make.stock.service.IStockKlineTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Component
public class StockKlineTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(StockKlineTaskExecutor.class);

    @Resource
    private IStockKlineTaskService stockKlineTaskService;

    @Resource
    private StockKlineProcessor stockKlineProcessor;

    @Resource
    private StockKlineRepositoryService repositoryService;

    @Resource
    private WatchStockUpdater watchStockUpdater;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void executeAll(int nodeId, String traceId) {
        long globalStart = System.currentTimeMillis();

        List<StockKlineTask> taskList = stockKlineTaskService.getStockAllTask(nodeId);
        if (taskList == null || taskList.isEmpty()) {
            log.info("❌ [Kline] 没有任务 TraceId={} NodeId={}", traceId, nodeId);
            return;
        }

        log.info("🏁 [Kline] 任务开始 TraceId={} NodeId={} 任务数={}", traceId, nodeId, taskList.size());

        // 分批处理，每批 20 个
        List<List<StockKlineTask>> partitions = Lists.partition(taskList, 20);
        ExecutorService executor = ThreadPoolUtil.getWatchStockExecutor();

        int successCount = 0;
        int failCount = 0;

        for (List<StockKlineTask> batch : partitions) {
            List<Future<ProcessResult>> futures = new ArrayList<>();
            // 提交批次任务
            for (StockKlineTask task : batch) {
                futures.add(executor.submit(() -> stockKlineProcessor.processTaskData(task, DF)));
            }

            // 收集批次结果
            List<StockKline> batchInsert = new ArrayList<>();
            List<StockKline> batchUpdate = new ArrayList<>();
            List<Long> batchSuccessIds = new ArrayList<>();

            for (int i = 0; i < futures.size(); i++) {
                try {
                    ProcessResult r = futures.get(i).get(5, TimeUnit.HOURS); // 单个任务超时
                    if (r.success) {
                        batchInsert.addAll(r.insertList);
                        batchUpdate.addAll(r.updateList);
                        batchSuccessIds.add(batch.get(i).getId());
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ [Kline] 单任务异常 TraceId={} Code={} err={}",
                            traceId, batch.get(i).getStockCode(), e);
                }
            }

            // 批次落库
            if (!batchInsert.isEmpty()) {
                repositoryService.insertOrUpdateBatch(batchInsert);
            }
            if (!batchUpdate.isEmpty()) {
                repositoryService.batchUpdateByStockCodeAndTradeDate(batchUpdate);
                // 更新 WatchStock
                for (StockKline k : batchUpdate) {
                    watchStockUpdater.processWatchStock(k);
                }
            }
            if (!batchSuccessIds.isEmpty()) {
                stockKlineTaskService.batchFinishTask(batchSuccessIds);
            }
        }

        long cost = System.currentTimeMillis() - globalStart;
        log.info("🏁 [Kline] 任务结束 TraceId={} NodeId={} 成功={} 失败={} 耗时={} ms",
                traceId, nodeId, successCount, failCount, cost);
    }
}
