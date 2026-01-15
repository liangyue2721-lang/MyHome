package com.make.stock.service.scheduled.impl;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.make.common.constant.KafkaTopics;
import com.make.stock.domain.StockRefreshTask;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import com.make.common.core.NodeRegistry;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 关注股票数据处理器 (Producer)
 * <p>
 * 改造后：
 * 1. 负责生产任务
 * 2. 负责启动循环 (SmartLifecycle)
 * 3. 负责触发下一轮 (Trigger Loop)
 * 4. 仅 Master 节点执行生产
 * </p>
 */
@Component
public class StockWatchProcessor implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockWatchProcessor.class);

    @Resource
    private IWatchstockService watchStockService;

    @Resource
    private StockTaskQueueService stockTaskQueueService;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    private NodeRegistry nodeRegistry;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * SmartLifecycle: Start the loop on boot
     */
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("StockWatchProcessor started. Triggering first batch...");
            // Delay slightly to ensure system fully ready
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(this::triggerNextBatch);
        }
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1; // Start after core services, before consumers
    }

    /**
     * 触发下一轮批次 (Async)
     */
    public void triggerNextBatch() {
        if (!running.get()) return;

        CompletableFuture.runAsync(() -> {
            String nextTraceId = UUID.randomUUID().toString().replace("-", "");
            log.info("Triggering next batch loop. TraceId={}", nextTraceId);
            processTask(nextTraceId);
        }).exceptionally(e -> {
            log.error("Failed to trigger next batch", e);
            return null;
        });
    }

    /**
     * 入口：生产自选股任务
     */
    public void processTask(String traceId) {
        // Master Check
        if (!nodeRegistry.isMaster()) {
            log.debug("[StockWatchProcessor] Skipping task production (Not Master). TraceId={}", traceId);
            // Even if not master, we must keep the loop alive to check later (e.g. if master fails over)
            try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException ignored) {}
            triggerNextBatch();
            return;
        }

        long start = System.currentTimeMillis();
        List<Watchstock> watchstocks = watchStockService.selectWatchstockList(null);

        if (watchstocks == null || watchstocks.isEmpty()) {
            log.warn("自选股任务生产结束：没有需要更新的股票 TraceId={}", traceId);
            // If empty, we should probably sleep and retry to keep loop alive?
            // For now, let's trigger next batch with delay
             try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException ignored) {}
             triggerNextBatch();
            return;
        }

        // Init Batch Counter
        stockTaskQueueService.initBatch(traceId, watchstocks.size());

        log.info("=====【自选股任务生产开始】===== TraceId={} 总数：{}", traceId, watchstocks.size());

        for (Watchstock ws : watchstocks) {
            try {
                StockRefreshTask task = new StockRefreshTask();
                task.setTaskId(UUID.randomUUID().toString());
                task.setStockCode(ws.getCode());
                task.setTaskType("REFRESH_PRICE");
                task.setCreateTime(System.currentTimeMillis());
                task.setTraceId(traceId);

                // Use stockCode as key to ensure same stock goes to same partition
                kafkaTemplate.send(KafkaTopics.TOPIC_STOCK_REFRESH, ws.getCode(), JSON.toJSONString(task));
            } catch (Exception e) {
                log.error("生产任务失败: {}", ws.getCode(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=====【自选股任务生产结束】===== TraceId={} 总耗时={} ms , 投递数量={}",
                traceId, cost, watchstocks.size());
    }
}
