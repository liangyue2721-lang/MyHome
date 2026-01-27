package com.make.stock.service.scheduled.impl;

import com.alibaba.fastjson2.JSON;
import com.make.common.constant.KafkaTopics;
import com.make.common.core.NodeRegistry;
import com.make.stock.domain.Watchstock;
import com.make.stock.domain.dto.StockTickTaskDTO;
import com.make.stock.service.IWatchstockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 股票逐笔成交任务调度器 (Producer)
 * 定时扫描关注股票，生成逐笔成交抓取任务
 */
@Component
public class StockTickTaskScheduler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockTickTaskScheduler.class);

    @Resource
    private IWatchstockService watchstockService;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    private NodeRegistry nodeRegistry;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(15, 30);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("StockTickTaskScheduler started.");
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TickScheduler");
                t.setDaemon(true);
                return t;
            });

            // Run every 5 minutes (Initial delay 60s)
            scheduler.scheduleWithFixedDelay(this::runSchedule, 60, 300, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdown();
        }
        log.info("StockTickTaskScheduler stopped.");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }

    public void runSchedule() {
        if (!running.get()) return;

        // Master Check
        if (!nodeRegistry.isMaster()) {
            return;
        }

        // Time Range Check
        LocalTime now = LocalTime.now();
        if (now.isBefore(START_TIME) || now.isAfter(END_TIME)) {
            return;
        }

        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            log.info("[TickScheduler] Triggering tick fetch tasks. TraceId={}", traceId);

            List<Watchstock> stocks = watchstockService.selectWatchstockList(new Watchstock());
            if (stocks == null || stocks.isEmpty()) return;

            for (Watchstock ws : stocks) {
                String stockCode = ws.getCode();
                if (stockCode == null) continue;

                String market = deriveMarket(ws);

                StockTickTaskDTO task = new StockTickTaskDTO(traceId, stockCode, market);
                String json = JSON.toJSONString(task);

                kafkaTemplate.send(KafkaTopics.TOPIC_STOCK_TICK_TASK, stockCode, json);
            }
        } catch (Exception e) {
            log.error("[TickScheduler] Failed to schedule tasks", e);
        }
    }

    private String deriveMarket(Watchstock ws) {
        String api = ws.getStockApi();
        if (api != null) {
            if (api.contains("secid=1.")) return "1";
            if (api.contains("secid=0.")) return "0";
        }

        // Fallback based on code prefix
        String code = ws.getCode();
        if (code != null && code.startsWith("6")) {
            return "1";
        }
        return "0";
    }
}
