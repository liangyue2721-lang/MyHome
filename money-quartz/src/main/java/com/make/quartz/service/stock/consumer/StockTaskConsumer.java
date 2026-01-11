package com.make.quartz.service.stock.consumer;

import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.ip.IpUtils;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.domain.StockRefreshTask;
import com.make.quartz.service.impl.StockWatchProcessor;
import com.make.quartz.service.stock.handler.IStockRefreshHandler;
import com.make.quartz.service.stock.queue.StockTaskQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 股票刷新任务消费者（升级版：多线程并发消费 + 内部失败重试）
 * <p>
 * Refactored:
 * 1. Logic delegated to StockRefreshHandler.
 * 2. Focuses on Queue Polling, Concurrency, and Flow Control.
 */
@Component
public class StockTaskConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockTaskConsumer.class);

    private static final long EMPTY_QUEUE_SLEEP_MS = 200;
    private static final int LOCK_ATTEMPTS = 2;
    private static final long LOCK_RETRY_SLEEP_MS = 80;

    @Resource
    private StockTaskQueueService queueService;

    // Lazy inject to avoid circular dependency (Consumer -> Processor -> Consumer)
    @Resource
    @org.springframework.context.annotation.Lazy
    private StockWatchProcessor stockWatchProcessor;

    @Resource
    private QuartzProperties quartzProperties;

    @Resource
    private IStockRefreshHandler stockRefreshHandler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private String currentNodeId;
    private ThreadPoolExecutor executePool;
    private Semaphore submitLimiter;
    private ExecutorService pollPool;

    @PostConstruct
    public void init() {
        this.currentNodeId = IpUtils.getHostIp();
        this.executePool = (ThreadPoolExecutor) ThreadPoolUtil.getWatchStockExecutor();

        int inFlightLimit = Math.max(1, executePool.getMaximumPoolSize());
        this.submitLimiter = new Semaphore(inFlightLimit);

        int pollWorkers = quartzProperties.getStockPollWorkers();
        this.pollPool = Executors.newFixedThreadPool(pollWorkers, r -> {
            Thread t = new Thread(r);
            t.setName("stock-task-poll-worker");
            t.setDaemon(true);
            return t;
        });

        log.info("StockTaskConsumer init done. node={}, pollWorkers={}, inFlightLimit={}, executePool(max={})",
                currentNodeId, pollWorkers, inFlightLimit, executePool.getMaximumPoolSize());
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                queueService.recoverWaitingTasks();
            } catch (Exception e) {
                log.error("Failed to recover waiting tasks asynchronously", e);
            }
        });

        int pollWorkers = quartzProperties.getStockPollWorkers();
        for (int i = 0; i < pollWorkers; i++) {
            pollPool.submit(this::pollLoopSafely);
        }

        log.info("StockTaskConsumer started. node={}, pollWorkers={}", currentNodeId, pollWorkers);
    }

    @Override
    public void stop() {
        running.set(false);
        pollPool.shutdownNow();
        log.info("StockTaskConsumer stopped. node={}", currentNodeId);
    }

    @PreDestroy
    public void destroy() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void pollLoopSafely() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnceAndSubmit();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Poll loop unexpected error", e);
                sleepQuiet(300);
            }
        }
    }

    private void pollOnceAndSubmit() throws InterruptedException {
        StockRefreshTask task = queueService.poll();

        if (task == null) {
            TimeUnit.MILLISECONDS.sleep(EMPTY_QUEUE_SLEEP_MS);
            return;
        }

        submitLimiter.acquire();

        try {
            executePool.execute(() -> {
                try {
                    handleTaskExecution(task);
                } finally {
                    submitLimiter.release();
                }
            });
        } catch (RejectedExecutionException ree) {
            submitLimiter.release();
            log.warn("Execute pool rejected task. stockCode={}, traceId={}", task.getStockCode(), task.getTraceId(), ree);
        }
    }

    private void handleTaskExecution(StockRefreshTask task) {
        final String stockCode = task.getStockCode();
        final String traceId = task.getTraceId();

        if (isBlank(stockCode)) {
            log.warn("Skip invalid task: empty stockCode, traceId={}", traceId);
            return;
        }

        boolean locked = tryLockWithRetry(stockCode);
        if (!locked) {
            return;
        }

        try {
            // Business Logic Delegated to Handler
            stockRefreshHandler.refreshStock(task);
        } finally {
            safeReleaseLock(stockCode);

            queueService.deleteStatus(stockCode, traceId);

            long remaining = queueService.decrementBatch(traceId);
            if (remaining == 0) {
                log.info("Batch completed (traceId={}). Triggering next batch...", traceId);
                stockWatchProcessor.triggerNextBatch();
            } else if (remaining < 0) {
                log.warn("Batch counter missing (remaining < 0). Attempting recovery... traceId={}", traceId);
                if (queueService.tryLockRecovery(traceId)) {
                    log.info("Recovery lock acquired. Triggering next batch... traceId={}", traceId);
                    stockWatchProcessor.triggerNextBatch();
                }
            }
        }
    }

    private boolean tryLockWithRetry(String stockCode) {
        for (int i = 1; i <= LOCK_ATTEMPTS; i++) {
            if (queueService.tryLockStock(stockCode, currentNodeId)) {
                return true;
            }
            sleepQuiet(LOCK_RETRY_SLEEP_MS);
        }
        return false;
    }

    private void safeReleaseLock(String stockCode) {
        try {
            queueService.releaseLock(stockCode, currentNodeId);
        } catch (Exception e) {
            log.error("Release lock failed: stockCode={}", stockCode, e);
        }
    }

    private void sleepQuiet(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
