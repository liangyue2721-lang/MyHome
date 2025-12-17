package com.make.quartz.service.impl;

import com.make.common.utils.ThreadPoolUtil;
import com.make.quartz.service.IStockTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 股票K线任务入口服务实现类
 * <p>
 * 该类作为股票K线任务的统一入口，负责接收来自Quartz或其他调度器的任务请求，
 * 并将其转发给具体的任务执行器进行处理。此类采用了极简设计模式，仅暴露核心功能接口。
 * </p>
 */
@Service
public class IStockTaskServiceImpl implements IStockTaskService {

    private static final Logger log = LoggerFactory.getLogger(IStockTaskServiceImpl.class);
    /**
     * 股票K线任务执行器实例
     * <p>通过构造函数注入，用于实际执行股票K线任务</p>
     */
    private final StockKlineTaskExecutor taskExecutor;

    /**
     * 构造函数注入StockKlineTaskExecutor依赖
     *
     * @param taskExecutor 股票K线任务执行器实例
     */
    @Autowired
    public IStockTaskServiceImpl(StockKlineTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }


    /**
     * 股票K线处理器，用于处理具体的股票K线任务
     */
    @Resource
    private StockWatchProcessor stockWatchProcessor;
    @Resource
    private StockETFrocessor stockETFrocessor;

    /**
     * 运行股票K线任务的统一入口
     *
     * <p>职责说明：</p>
     * <ul>
     *   <li>作为调度任务的入口，由 Quartz 或其他调度器调用</li>
     *   <li>支持集群节点，nodeId 用于任务分片或并发控制</li>
     *   <li>在执行入口中调度两个任务：
     *      <ul>
     *        <li>自选股实时数据更新（watchProcessor.processTask）</li>
     *        <li>历史K线任务执行（taskExecutor.executeAll）</li>
     *      </ul>
     *   </li>
     *   <li>通过线程池异步处理自选股行情，提高整体任务执行性能</li>
     * </ul>
     *
     * @param nodeId 当前执行任务的节点标识（用于集群/分片执行）
     */
    @Override
    public void runStockKlineTask(int nodeId) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");

        log.info("=====【股票K线任务开始】NodeId={} TraceId={} =====", nodeId, traceId);

        // 并行执行三个子任务
        // 1. 自选股实时数据更新
        CompletableFuture<Void> watchTask = CompletableFuture.runAsync(() -> {
            try {
                log.info("[WatchProcessor-Start] TraceId={}", traceId);
                stockWatchProcessor.processTask(traceId);
                log.info("[WatchProcessor-End] TraceId={}", traceId);
            } catch (Exception e) {
                log.error("[WatchProcessor-Error] TraceId={} err={}", traceId, e.getMessage(), e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor());

        // 2. ETF 实时数据更新
        CompletableFuture<Void> etfTask = CompletableFuture.runAsync(() -> {
            try {
                log.info("[ETFProcessor-Start] TraceId={}", traceId);
                stockETFrocessor.processTask(traceId);
                log.info("[ETFProcessor-End] TraceId={}", traceId);
            } catch (Exception e) {
                log.error("[ETFProcessor-Error] TraceId={} err={}", traceId, e.getMessage(), e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor());

        // 3. 历史K线更新任务
        CompletableFuture<Void> klineTask = CompletableFuture.runAsync(() -> {
            try {
                log.info("[TaskExecutor-Start] NodeId={} TraceId={}", nodeId, traceId);
                taskExecutor.executeAll(nodeId, traceId);
                log.info("[TaskExecutor-End] NodeId={} TraceId={}", nodeId, traceId);
            } catch (Exception e) {
                log.error("[TaskExecutor-Error] NodeId={} TraceId={} err={}", nodeId, traceId, e.getMessage(), e);
            }
        }, ThreadPoolUtil.getWatchStockExecutor());

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(watchTask, etfTask, klineTask).join();
        } catch (Exception e) {
            log.error("=====【股票K线任务执行异常】 NodeId={} TraceId={} err={}", nodeId, traceId, e.getMessage(), e);
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=====【股票K线任务结束】 NodeId={} TraceId={} , 总耗时={} ms =====",
                nodeId, traceId, cost);
    }
}
