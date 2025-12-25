package com.make.quartz.service.impl;

import com.google.common.collect.Lists;
import com.make.quartz.domain.StockRefreshTask;
import com.make.quartz.service.stock.queue.StockTaskQueueService;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

/**
 * 关注股票数据处理器 (Producer)
 * <p>
 * 改造后：仅负责将股票刷新任务投递到 Redis 队列，不再直接执行。
 * </p>
 */
@Component
public class StockWatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(StockWatchProcessor.class);

    @Resource
    private IWatchstockService watchStockService;

    @Resource
    private StockTaskQueueService stockTaskQueueService;

    /**
     * 入口：生产自选股任务
     */
    public void processTask(String traceId) {
        long start = System.currentTimeMillis();
        List<Watchstock> watchstocks = watchStockService.selectWatchstockList(null);

        if (watchstocks == null || watchstocks.isEmpty()) {
            log.warn("自选股任务生产结束：没有需要更新的股票 TraceId={}", traceId);
            return;
        }

        log.info("=====【自选股任务生产开始】===== TraceId={} 总数：{}", traceId, watchstocks.size());

        for (Watchstock ws : watchstocks) {
            try {
                StockRefreshTask task = new StockRefreshTask();
                task.setTaskId(UUID.randomUUID().toString());
                task.setStockCode(ws.getCode());
                task.setTaskType("REFRESH_PRICE");
                task.setCreateTime(System.currentTimeMillis());
                task.setTraceId(traceId);

                stockTaskQueueService.enqueue(task);
            } catch (Exception e) {
                log.error("生产任务失败: {}", ws.getCode(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=====【自选股任务生产结束】===== TraceId={} 总耗时={} ms , 投递数量={}",
                traceId, cost, watchstocks.size());
    }
}
