/**
 * 股票K线任务执行器
 * <p>
 * 该类负责执行所有股票K线数据处理任务，采用 SmartLifecycle 自驱动模式，
 * 循环获取待处理任务并提交到线程池执行。
 * <p>
 * 主要功能包括：
 * - 启动后台 Daemon 线程循环获取任务 (Loop)
 * - 使用信号量控制并发数 (Semaphore)
 * - 提交任务到 Core Executor 执行
 * - 处理结果并更新任务状态 (Status 3)
 */
package com.make.stock.service.scheduled.impl;

import com.google.common.collect.Lists;
import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.StockKlineTask;
import com.make.stock.domain.dto.ProcessResult;
import com.make.stock.service.IStockKlineTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StockKlineTaskExecutor implements SmartLifecycle {

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

    // Lifecycle control
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService mainLoopExecutor;

    // Concurrency control (Max 5 concurrent tasks)
    private final Semaphore semaphore = new Semaphore(5);

    // In-memory set to prevent duplicate submissions of the same task ID
    private final Set<Long> processingIds = ConcurrentHashMap.newKeySet();

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("🚀 [KlineExecutor] Starting background loop...");
            mainLoopExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "KlineTask-Loop");
                t.setDaemon(true);
                return t;
            });
            mainLoopExecutor.submit(this::runWorker);
        }
    }

    @Override
    public void stop() {
        log.info("🛑 [KlineExecutor] Stopping...");
        running.set(false);
        if (mainLoopExecutor != null) {
            mainLoopExecutor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // Start late
    }

    /**
     * Main Worker Loop
     */
    private void runWorker() {
        while (running.get()) {
            try {
                // 1. Acquire Permit (Blocking if full)
                if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                    continue; // Loop check running state
                }

                // 2. Fetch Pending Task (Limit 1 to allow fair distribution if scaled later, or simple loop)
                // Note: selectPendingTaskLimit orders by update_time asc, so it acts as a queue.
                List<StockKlineTask> tasks = stockKlineTaskService.selectPendingTaskLimit(1);

                if (tasks == null || tasks.isEmpty()) {
                    semaphore.release();
                    // No tasks, sleep for a while
                    TimeUnit.SECONDS.sleep(5);
                    continue;
                }

                StockKlineTask task = tasks.get(0);
                Long taskId = task.getId();

                // 3. Duplicate Check
                if (processingIds.contains(taskId)) {
                    // Already processing this ID (Concurrency Edge Case: query returned it before update commit?)
                    // Skip and release
                    semaphore.release();
                    // Sleep tiny bit to let DB catch up or allow other tasks to surface
                    TimeUnit.MILLISECONDS.sleep(100);
                    continue;
                }

                // 4. Claim Task (Synchronously Update Time -> Move to End of Queue)
                try {
                    processingIds.add(taskId);
                    stockKlineTaskService.updateTaskTime(taskId);
                } catch (Exception e) {
                    log.error("[KlineExecutor] Claim Failed", e);
                    processingIds.remove(taskId);
                    semaphore.release();
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }

                // 5. Submit to Thread Pool
                try {
                    ThreadPoolUtil.getCoreExecutor().execute(() -> processSingleTask(task));
                } catch (Exception e) {
                    // If submission fails (e.g. RejectedExecution), we must release the permit immediately
                    log.error("[KlineExecutor] Submission Failed", e);
                    processingIds.remove(taskId);
                    semaphore.release();
                }

            } catch (InterruptedException e) {
                log.warn("[KlineExecutor] Interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[KlineExecutor] Loop Error", e);
                // If we are here, it means fetch failed (permit acquired but not released/submitted)
                // We should release to avoid leak
                semaphore.release();
                try { TimeUnit.SECONDS.sleep(5); } catch (InterruptedException ie) {}
            }
        }
    }

    /**
     * Process Single Task (Async)
     */
    private void processSingleTask(StockKlineTask task) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        try {
            // log.info("▶ [Kline] Start Task: {} Trace: {}", task.getStockCode(), traceId);

            // Execute Processor
            ProcessResult result = stockKlineProcessor.processTaskData(task, DF);

            // Persist
            if (result.success) {
                if (!result.insertList.isEmpty()) {
                    repositoryService.insertOrUpdateBatch(result.insertList);
                }
                if (!result.updateList.isEmpty()) {
                    repositoryService.batchUpdateByStockCodeAndTradeDate(result.updateList);
                    for (StockKline k : result.updateList) {
                        watchStockUpdater.processWatchStock(k);
                    }
                }
                // Update Task Status (Done/Re-queued)
                // Calling batchFinishTask updates status to 3 and update_time to now.
                // This effectively re-queues it for the next day/cycle.
                stockKlineTaskService.batchFinishTask(Collections.singletonList(task.getId()));

                log.info("✓ [Kline] Finished: {} Insert: {} Update: {} Trace: {}",
                        task.getStockCode(), result.insertList.size(), result.updateList.size(), traceId);
            } else {
                log.warn("✕ [Kline] Failed: {} Trace: {}", task.getStockCode(), traceId);
                // Failure: We already touched update_time in claim step.
                // So it is already re-queued at the back.
                // We do NOT call batchFinishTask (which would set Status 3).
                // Keep status as is (0 or 3), but time is updated so it won't be picked up immediately.
            }

        } catch (Exception e) {
            log.error("❌ [Kline] Error: {} Trace: {}", task.getStockCode(), traceId, e);
        } finally {
            // Cleanup
            processingIds.remove(task.getId());
            semaphore.release();
        }
    }
}
