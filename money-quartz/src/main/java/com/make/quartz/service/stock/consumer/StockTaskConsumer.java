package com.make.quartz.service.stock.consumer;

import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.ip.IpUtils;
import com.make.quartz.domain.StockRefreshTask;
import com.make.quartz.domain.StockTaskStatus;
import com.make.quartz.service.impl.WatchStockUpdater;
import com.make.quartz.service.stock.queue.StockTaskQueueService;
import com.make.stock.domain.Watchstock;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.IWatchstockService;
import com.make.stock.util.KlineDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 股票刷新任务消费者
 * 逻辑：Pop -> Lock -> Fetch -> Update -> Release
 */
@Component
public class StockTaskConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockTaskConsumer.class);

    @Resource
    private StockTaskQueueService queueService;

    @Resource
    private WatchStockUpdater watchStockUpdater;

    @Resource
    private IWatchstockService watchstockService;

    private volatile boolean running = false;
    private ThreadPoolExecutor consumerExecutor;
    private String currentNodeId;

    @PostConstruct
    public void init() {
        this.currentNodeId = IpUtils.getHostIp();
        // 使用单线程或小线程池轮询
        // 由于是 IO 密集型 (Fetch HTTP), 可以开多一点线程来并发消费
        // 但要求 "FIFO per Node" -> 其实并发也是 FIFO 取任务，执行完成顺序不一定
        // 需求说： "多节点并行消费时，各节点各自遵循 FIFO 语义" -> 意味着一个节点内部可以是串行，也可以并行
        // "占用线程池中的一个线程执行" -> 暗示并发
        // 这里使用 关注股票线程池 或 独立线程池。为了隔离，建议使用独立线程池或复用 watchStockExecutor
        // 这里为了简单和可控，开启一个 Loop 线程取任务，提交到 watchStockExecutor 执行

        this.consumerExecutor = (ThreadPoolExecutor) ThreadPoolUtil.getWatchStockExecutor();
    }

    private void startLoop() {
        Thread loopThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    StockRefreshTask task = queueService.poll();
                    if (task == null) {
                        Thread.sleep(500); // Empty queue wait
                        continue;
                    }

                    // Got task, submit to executor with backpressure
                    while (running) {
                        try {
                            consumerExecutor.execute(() -> processTask(task));
                            break; // Success
                        } catch (java.util.concurrent.RejectedExecutionException re) {
                            // Backpressure: Sleep and retry
                            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                        } catch (Exception e) {
                            log.warn("Submit task failed, task lost: {}", task.getStockCode());
                            break;
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Consumer loop error", e);
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }, "stock-task-consumer-loop");
        loopThread.setDaemon(true);
        loopThread.start();

        log.info("StockTaskConsumer started on node: {}", currentNodeId);
    }

    private void processTask(StockRefreshTask task) {
        String stockCode = task.getStockCode();
        String traceId = task.getTraceId();

        // 1. Try Lock
        if (!queueService.tryLockStock(stockCode, currentNodeId)) {
            // Locked by others
            updateStatus(stockCode, StockTaskStatus.STATUS_SKIPPED, "Occupied by other node", traceId);
            // ACK (Implicitly done by pop)
            return;
        }

        long start = System.currentTimeMillis();
        try {
            updateStatus(stockCode, StockTaskStatus.STATUS_RUNNING, null, traceId);

            // 2. Load WatchStock info (need API url)
            Watchstock ws = watchstockService.getWatchStockByCode(stockCode);
            if (ws == null) {
                updateStatus(stockCode, StockTaskStatus.STATUS_FAILED, "Stock not found in DB", traceId);
                return;
            }

            // 3. Fetch Realtime
            StockRealtimeInfo info = KlineDataFetcher.fetchRealtimeInfo(ws.getStockApi());
            if (info != null) {
                // 4. Update DB
                watchStockUpdater.updateFromRealtimeInfo(ws, info);
                // 5. Save
                watchstockService.updateWatchstock(ws);

                updateStatus(stockCode, StockTaskStatus.STATUS_SUCCESS, "Price: " + info.getPrice(), traceId);
            } else {
                updateStatus(stockCode, StockTaskStatus.STATUS_FAILED, "Fetch returned null", traceId);
            }

        } catch (Exception e) {
            log.error("Task failed: {}", stockCode, e);
            updateStatus(stockCode, StockTaskStatus.STATUS_FAILED, e.getMessage(), traceId);
        } finally {
            // 6. Release Lock
            queueService.releaseLock(stockCode, currentNodeId);
            // Log cost?
        }
    }

    private void updateStatus(String stockCode, String status, String result, String traceId) {
        StockTaskStatus s = new StockTaskStatus();
        s.setStockCode(stockCode);
        s.setStatus(status);
        s.setOccupiedByNode(currentNodeId);
        s.setOccupiedTime(System.currentTimeMillis());
        s.setTraceId(traceId);
        s.setLastResult(result);
        queueService.updateStatus(stockCode, s);
    }

    @Override
    public void start() {
        this.running = true;
        startLoop();
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
