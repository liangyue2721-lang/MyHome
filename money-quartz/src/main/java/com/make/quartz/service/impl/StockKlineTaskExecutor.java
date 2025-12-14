package com.make.quartz.service.impl;

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

    public void executeAll(int nodeId) {
        long globalStart = System.currentTimeMillis();

        List<StockKlineTask> taskList = stockKlineTaskService.getStockAllTask(nodeId);
        if (taskList == null || taskList.isEmpty()) {
            log.info("❌ 没有任务（nodeId={})", nodeId);
            return;
        }

        ExecutorService executor = ThreadPoolUtil.getWatchStockExecutor();
        List<Future<ProcessResult>> futures = new ArrayList<>();

        for (StockKlineTask task : taskList) {
            futures.add(executor.submit(() -> stockKlineProcessor.processTaskData(task, DF)));
        }

        // 聚合
        List<StockKline> globalInsert = new ArrayList<>();
        List<StockKline> globalUpdate = new ArrayList<>();
        List<Long> successTasks = new ArrayList<>();
        int failedTasks = 0;

        for (int i = 0; i < futures.size(); i++) {
            Future<ProcessResult> f = futures.get(i);
            StockKlineTask task = taskList.get(i);

            try {
                ProcessResult r = f.get(30, TimeUnit.MINUTES);

                if (r.success) {
                    globalInsert.addAll(r.insertList);
                    globalUpdate.addAll(r.updateList);
                    successTasks.add(task.getId());
                } else {
                    failedTasks++;
                }

            } catch (Exception e) {
                failedTasks++;
                log.error("❌ 单任务执行异常 {}", task.getStockCode(), e);
            }
        }

        // ---------------------
        // 一次性落库（insert + update）
        // ---------------------

        if (!globalInsert.isEmpty())
            repositoryService.insertOrUpdateBatch(globalInsert);

        if (!globalUpdate.isEmpty())
            repositoryService.batchUpdateByStockCodeAndTradeDate(globalUpdate);

        // ---------------------
        // watchStock（只针对更新记录）
        // ---------------------

        for (StockKline k : globalUpdate) {
            watchStockUpdater.processWatchStock(k);
        }

        // ---------------------
        // 批量更新任务状态
        // ---------------------

        if (!successTasks.isEmpty()) {
            stockKlineTaskService.batchFinishTask(successTasks);
        }

        long cost = System.currentTimeMillis() - globalStart;
        log.info("🏁【节点完成】任务={} 成功={} 失败={} 耗时={} ms",
                taskList.size(), successTasks.size(), failedTasks, cost);
    }
}
