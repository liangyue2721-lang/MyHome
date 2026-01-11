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
 * 股票刷新任务消费者（多线程并发消费）
 * <p>
 * 重构说明：
 * 1. 业务逻辑已委托给 StockRefreshHandler 处理。
 * 2. 本类专注于队列轮询（Polling）、并发控制与分布式锁管理。
 */
@Component
public class StockTaskConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockTaskConsumer.class);

    private static final long EMPTY_QUEUE_SLEEP_MS = 200;
    private static final int LOCK_ATTEMPTS = 2;
    private static final long LOCK_RETRY_SLEEP_MS = 80;

    @Resource
    private StockTaskQueueService queueService;

    // 懒加载注入，避免循环依赖 (Consumer -> Processor -> Consumer)
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

    /**
     * 初始化消费者资源
     * 包括：执行线程池、并发限制信号量、轮询线程池
     */
    @PostConstruct
    public void init() {
        this.currentNodeId = IpUtils.getHostIp();
        this.executePool = (ThreadPoolExecutor) ThreadPoolUtil.getWatchStockExecutor();

        // 使用线程池最大线程数作为并发上限
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

    /**
     * 启动消费者
     * 1. 恢复 WAITING 状态的任务
     * 2. 启动轮询线程
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        // 异步恢复积压任务，避免阻塞启动
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

    /**
     * 停止消费者
     */
    @Override
    public void stop() {
        running.set(false);
        pollPool.shutdownNow();
        log.info("StockTaskConsumer stopped. node={}", currentNodeId);
    }

    /**
     * 销毁前确保停止
     */
    @PreDestroy
    public void destroy() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 设定启动优先级（最晚启动）
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * 安全的轮询循环
     * 捕获异常以防止轮询线程意外退出
     */
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

    /**
     * 单次轮询并提交任务
     * 包含背压控制（Backpressure）
     */
    private void pollOnceAndSubmit() throws InterruptedException {
        StockRefreshTask task = queueService.poll();

        if (task == null) {
            TimeUnit.MILLISECONDS.sleep(EMPTY_QUEUE_SLEEP_MS);
            return;
        }

        // 获取许可，控制并发量
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

    /**
     * 处理单个任务的执行流程
     * 包含：分布式锁、业务执行、状态清理、批次计数
     */
    private void handleTaskExecution(StockRefreshTask task) {
        final String stockCode = task.getStockCode();
        final String traceId = task.getTraceId();

        if (isBlank(stockCode)) {
            log.warn("Skip invalid task: empty stockCode, traceId={}", traceId);
            return;
        }

        // 尝试获取分布式锁
        boolean locked = tryLockWithRetry(stockCode);
        if (!locked) {
            return;
        }

        try {
            // 委托给 Handler 执行具体业务
            stockRefreshHandler.refreshStock(task);
        } finally {
            safeReleaseLock(stockCode);

            // 清理状态并递减批次计数
            queueService.deleteStatus(stockCode, traceId);

            long remaining = queueService.decrementBatch(traceId);
            if (remaining == 0) {
                log.info("Batch completed (traceId={}). Triggering next batch...", traceId);
                stockWatchProcessor.triggerNextBatch();
            } else if (remaining < 0) {
                log.warn("Batch counter missing (remaining < 0). Attempting recovery... traceId={}", traceId);
                // 尝试获取恢复锁以触发下一批次
                if (queueService.tryLockRecovery(traceId)) {
                    log.info("Recovery lock acquired. Triggering next batch... traceId={}", traceId);
                    stockWatchProcessor.triggerNextBatch();
                }
            }
        }
    }

    /**
     * 尝试获取分布式锁（带重试）
     */
    private boolean tryLockWithRetry(String stockCode) {
        for (int i = 1; i <= LOCK_ATTEMPTS; i++) {
            if (queueService.tryLockStock(stockCode, currentNodeId)) {
                return true;
            }
            sleepQuiet(LOCK_RETRY_SLEEP_MS);
        }
        return false;
    }

    /**
     * 安全释放锁
     */
    private void safeReleaseLock(String stockCode) {
        try {
            queueService.releaseLock(stockCode, currentNodeId);
        } catch (Exception e) {
            log.error("Release lock failed: stockCode={}", stockCode, e);
        }
    }

    /**
     * 线程休眠（忽略中断异常）
     */
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
