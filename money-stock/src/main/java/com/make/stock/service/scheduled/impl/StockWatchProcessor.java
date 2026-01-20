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
    private java.util.concurrent.ScheduledExecutorService watchdogExecutor;

    /**
     * SmartLifecycle: Start the loop on boot
     */
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("StockWatchProcessor started. Starting Watchdog...");
            watchdogExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "StockWatchDog");
                t.setDaemon(true);
                return t;
            });
            // Run Watchdog every 30 seconds
            watchdogExecutor.scheduleWithFixedDelay(this::runWatchdog, 5, 30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (watchdogExecutor != null) {
            watchdogExecutor.shutdown();
        }
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
     * Submit a Single Task (Public, called by Consumer or Watchdog)
     * Triggers the "Next Task" for this specific stock.
     */
    public void submitTask(String stockCode) {
        if (!running.get()) return;

        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            // Mark active immediately (Refresh Heartbeat)
            stockTaskQueueService.refreshActiveState(stockCode, traceId);

            StockRefreshTask task = new StockRefreshTask();
            task.setTaskId(UUID.randomUUID().toString());
            task.setStockCode(stockCode);
            task.setTaskType("REFRESH_PRICE");
            task.setCreateTime(System.currentTimeMillis());
            task.setTraceId(traceId);

            // Use Redis Queue for reliable distribution (replacing Kafka for this loop)
            stockTaskQueueService.enqueue(task);
            log.debug("Submitted next task for stock: {}, traceId: {}", stockCode, traceId);
        } catch (Exception e) {
            log.error("Failed to submit task for stock: {}", stockCode, e);
        }
    }

    /**
     * Watchdog (Master Only)
     * Scans all stocks and restarts loop if inactive.
     */
    public void runWatchdog() {
        if (!running.get()) return;

        // Master Only
        if (!nodeRegistry.isMaster()) {
            return;
        }

        try {
            List<Watchstock> watchstocks = watchStockService.selectWatchstockList(null);
            if (watchstocks == null || watchstocks.isEmpty()) return;

            List<String> codes = new java.util.ArrayList<>();
            for (Watchstock ws : watchstocks) {
                codes.add(ws.getCode());
            }

            // Check Active States (Batch MGET)
            List<Boolean> activeStates = stockTaskQueueService.checkActiveStates(codes);
            int restarted = 0;

            for (int i = 0; i < codes.size(); i++) {
                // If checking failed (size mismatch), break or skip
                if (i >= activeStates.size()) break;

                boolean isActive = activeStates.get(i);
                if (!isActive) {
                    // Not active -> Restart Loop
                    submitTask(codes.get(i));
                    restarted++;
                }
            }

            if (restarted > 0) {
                log.info("[Watchdog] Restarted loops for {} inactive stocks (Total: {})", restarted, codes.size());
            }

        } catch (Exception e) {
            log.error("Watchdog failed", e);
        }
    }
}
