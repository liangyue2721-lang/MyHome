/**
 * 股票K线任务执行器
 * <p>
 * 该类负责执行所有股票K线数据处理任务，采用多线程分批处理机制，
 * 以提高大量任务处理的效率和稳定性。
 * <p>
 * 主要功能包括：
 * - 获取所有待处理的股票K线任务
 * - 将任务分批处理，每批最多20个任务
 * - 并发执行各批次内的任务
 * - 收集处理结果并进行数据持久化
 * - 更新相关监控股票信息
 * - 记录任务执行日志和统计信息
 */
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

    /**
     * 日志记录器，用于记录任务执行过程中的关键信息和错误
     */
    private static final Logger log = LoggerFactory.getLogger(StockKlineTaskExecutor.class);

    /**
     * 股票K线任务服务，用于获取和更新任务状态
     */
    @Resource
    private IStockKlineTaskService stockKlineTaskService;

    /**
     * 股票K线处理器，用于处理单个任务的数据
     */
    @Resource
    private StockKlineProcessor stockKlineProcessor;

    /**
     * 股票K线数据仓库服务，用于批量插入或更新K线数据
     */
    @Resource
    private StockKlineRepositoryService repositoryService;

    /**
     * 监控股票更新器，用于更新监控股票的相关信息
     */
    @Resource
    private WatchStockUpdater watchStockUpdater;

    /**
     * 日期格式化器，用于格式化日期为 "yyyy-MM-dd" 格式
     */
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 执行所有股票K线任务
     * <p>
     * 该方法获取指定节点的所有股票K线任务，并使用多线程分批处理的方式执行这些任务。
     * 任务处理结果将被持久化到数据库中，并更新监控股票的相关信息。
     *
     * @param nodeId  节点ID，用于标识当前执行任务的节点
     * @param traceId 跟踪ID，用于追踪任务执行过程中的日志
     */
    public void executeAll(int nodeId, String traceId) {
        // 记录任务执行开始时间，用于后续计算总耗时
        long globalStart = System.currentTimeMillis();

        // 从服务层获取指定节点的所有股票K线任务
        List<StockKlineTask> taskList = stockKlineTaskService.getStockAllTask(nodeId);
        // 如果没有待处理的任务，记录日志并直接返回
        if (taskList == null || taskList.isEmpty()) {
            log.info("❌ [Kline] 没有任务 TraceId={} NodeId={}", traceId, nodeId);
            return;
        }

        // 记录任务开始执行的日志，包括跟踪ID、节点ID和任务总数
        log.info("🏁 [Kline] 任务开始 TraceId={} NodeId={} 任务数={}", traceId, nodeId, taskList.size());

        // 将任务列表分批，每批最多20个任务，以提高并发处理效率
        List<List<StockKlineTask>> partitions = Lists.partition(taskList, 20);
        // 获取用于处理核心业务的线程池执行器
        ExecutorService executor = ThreadPoolUtil.getCoreExecutor();

        // 初始化成功和失败任务计数器
        int successCount = 0;
        int failCount = 0;

        // 遍历每个任务批次
        for (List<StockKlineTask> batch : partitions) {
            // 创建Future列表以存储提交的异步任务
            List<Future<ProcessResult>> futures = new ArrayList<>();
            // 遍历批次中的每个任务并提交到线程池执行
            for (StockKlineTask task : batch) {
                // 提交任务到线程池，每个任务调用处理器处理数据
                futures.add(executor.submit(() -> stockKlineProcessor.processTaskData(task, DF)));
            }

            // 创建列表以收集批次处理结果
            List<StockKline> batchInsert = new ArrayList<>();
            List<StockKline> batchUpdate = new ArrayList<>();
            List<Long> batchSuccessIds = new ArrayList<>();

            // 遍历Future列表，获取每个任务的执行结果
            for (int i = 0; i < futures.size(); i++) {
                try {
                    // 获取任务执行结果，设置5小时超时时间以避免长时间等待
                    ProcessResult r = futures.get(i).get(5, TimeUnit.HOURS);
                    // 检查任务是否执行成功
                    if (r.success) {
                        // 将需要插入的数据添加到插入列表
                        batchInsert.addAll(r.insertList);
                        // 将需要更新的数据添加到更新列表
                        batchUpdate.addAll(r.updateList);
                        // 记录成功完成的任务ID
                        batchSuccessIds.add(batch.get(i).getId());
                        // 增加成功计数
                        successCount++;
                    } else {
                        // 如果任务执行失败，增加失败计数
                        failCount++;
                    }
                } catch (Exception e) {
                    // 捕获任务执行过程中的异常，增加失败计数并记录错误日志
                    failCount++;
                    log.error("❌ [Kline] 单任务异常 TraceId={} Code={} err={}",
                            traceId, batch.get(i).getStockCode(), e);
                }
            }

            // 将批次中的插入数据批量持久化到数据库
            if (!batchInsert.isEmpty()) {
                repositoryService.insertOrUpdateBatch(batchInsert);
            }
            // 将批次中的更新数据批量更新到数据库
            if (!batchUpdate.isEmpty()) {
                repositoryService.batchUpdateByStockCodeAndTradeDate(batchUpdate);
                // 遍历更新的数据，更新监控股票的相关信息
                for (StockKline k : batchUpdate) {
                    // 处理监控股票更新逻辑
                    watchStockUpdater.processWatchStock(k);
                }
            }
            // 批量标记已完成的任务
            if (!batchSuccessIds.isEmpty()) {
                stockKlineTaskService.batchFinishTask(batchSuccessIds);
            }
        }

        // 计算任务执行总耗时
        long cost = System.currentTimeMillis() - globalStart;
        // 记录任务执行完成的日志，包括成功/失败数量和总耗时
        log.info("🏁 [Kline] 任务结束 TraceId={} NodeId={} 成功={} 失败={} 耗时={} ms",
                traceId, nodeId, successCount, failCount, cost);
    }
}
