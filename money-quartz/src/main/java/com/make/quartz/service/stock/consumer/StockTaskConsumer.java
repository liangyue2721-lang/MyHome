package com.make.quartz.service.stock.consumer;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.ip.IpUtils;
import com.make.finance.domain.AssetRecord;
import com.make.finance.domain.YearlyInvestmentSummary;
import com.make.finance.service.IYearlyInvestmentSummaryService;
import com.make.quartz.domain.StockRefreshExecuteRecord;
import com.make.quartz.domain.StockRefreshTask;
import com.make.quartz.domain.StockTaskStatus;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.service.IStockRefreshExecuteRecordService;
import com.make.quartz.service.impl.StockWatchProcessor;
import com.make.quartz.service.impl.WatchStockUpdater;
import com.make.quartz.service.stock.queue.StockTaskQueueService;
import com.make.quartz.util.email.SendEmail;
import com.make.stock.domain.SalesData;
import com.make.stock.domain.SellPriceAlerts;
import com.make.stock.domain.StockListingNotice;
import com.make.stock.domain.StockTrades;
import com.make.stock.domain.Watchstock;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.ISalesDataService;
import com.make.stock.service.ISellPriceAlertsService;
import com.make.stock.service.IStockTradesService;
import com.make.stock.service.IWatchstockService;
import com.make.stock.util.KlineDataFetcher;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 股票刷新任务消费者（升级版：多线程并发消费 + 内部失败重试）
 * <p>
 * Refactored:
 * 1. No longer calls deleteStatus() on completion.
 * 2. Calls updateStatus() with terminal state (SUCCESS/FAILED/SKIPPED) which sets a short TTL.
 */
@Component
public class StockTaskConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockTaskConsumer.class);

    /* ===================== 可配置参数（建议最终做成配置项） ===================== */

    /**
     * 单个任务内部最大重试次数（不含首次尝试；此处按“总尝试次数=MAX_ATTEMPTS”来写更直观）。
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 重试基础等待时间（指数/线性退避都可以，这里采用“线性递增”以减少复杂度）。
     */
    private static final long RETRY_BASE_DELAY_MS = 300;

    /**
     * 空队列时的等待时间，降低空转 CPU。
     */
    private static final long EMPTY_QUEUE_SLEEP_MS = 200;

    /**
     * 锁竞争时的短暂重试次数（避免瞬时竞争导致大量 SKIPPED）。
     */
    private static final int LOCK_ATTEMPTS = 2;

    /**
     * 锁竞争时每次重试等待时间。
     */
    private static final long LOCK_RETRY_SLEEP_MS = 80;

    /* ===================== 依赖注入 ===================== */

    @Resource
    private StockTaskQueueService queueService;

    // Lazy inject to avoid circular dependency (Consumer -> Processor -> Consumer)
    @Resource
    @org.springframework.context.annotation.Lazy
    private StockWatchProcessor stockWatchProcessor;

    @Resource
    private WatchStockUpdater watchStockUpdater;

    @Resource
    private IWatchstockService watchstockService;

    @Resource
    private IStockRefreshExecuteRecordService recordService;
    @Resource
    private IStockTradesService stockTradesService;        // 交易记录服务
    @Resource
    private ISellPriceAlertsService sellPriceAlertsService; // 股票信息服务
    @Resource
    private ISalesDataService salesDataService; // 折线图
    @Resource
    private QuartzProperties quartzProperties;
    @Resource
    private IYearlyInvestmentSummaryService yearlyInvestmentSummaryService;

    /* ===================== 运行时状态与线程资源 ===================== */
    /**
     * 日期格式化器：确保 yyyy-MM-dd，不包含时分秒
     * DateTimeFormatter 是线程安全的，不用额外同步
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 生命周期状态：使用 AtomicBoolean 便于多线程可见与 CAS 防重复启动。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 当前节点 ID：用于分布式锁标识（这里用 IP）。
     */
    private String currentNodeId;

    /**
     * 执行线程池：承载真正的任务执行（HTTP fetch + DB update）。
     * 说明：这里复用你现有的 watchStockExecutor；也可以改为独立线程池以隔离资源。
     */
    private ThreadPoolExecutor executePool;

    /**
     * 提交背压：用于限制“在飞任务数量”，避免 executePool 饱和后触发 Rejected 并导致 poll 线程 sleep。
     * acquire -> 提交任务 -> 任务 finally release
     */
    private Semaphore submitLimiter;

    /**
     * Poll Worker 池：仅负责从队列取任务并提交到 executePool。
     * 特点：Poll 与 Execute 解耦，poll 线程不做耗时 IO（只做轻量 poll + submit）。
     */
    private ExecutorService pollPool;

    @PostConstruct
    public void init() {
        // 获取节点标识（分布式锁 owner 维度）。
        this.currentNodeId = IpUtils.getHostIp();

        // 复用现有线程池：确保其队列大小、最大线程数等符合预期。
        this.executePool = (ThreadPoolExecutor) ThreadPoolUtil.getWatchStockExecutor();

        // 用最大并发线程数作为“在飞任务”上限的近似值；你也可以用 queue capacity + maxPool 做更精细上限。
        int inFlightLimit = Math.max(1, executePool.getMaximumPoolSize());
        this.submitLimiter = new Semaphore(inFlightLimit);

        int pollWorkers = quartzProperties.getStockPollWorkers();
        // 初始化 poll worker 池。
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
     * 启动消费者：启动多个 poll worker 并发拉取任务。
     * SmartLifecycle 会在 Spring 启动阶段调用。
     */
    @Override
    public void start() {
        // 避免重复 start。
        if (!running.compareAndSet(false, true)) {
            return;
        }

        // Recovery: WAITING tasks must be in queue.
        // Execute asynchronously to prevent blocking startup due to Redis timeout/latency
        CompletableFuture.runAsync(() -> {
            try {
                queueService.recoverWaitingTasks();
            } catch (Exception e) {
                log.error("Failed to recover waiting tasks asynchronously", e);
            }
        });

        int pollWorkers = quartzProperties.getStockPollWorkers();
        // 启动 N 个 poll loop，提高拉取速度，减少队列堆积。
        for (int i = 0; i < pollWorkers; i++) {
            pollPool.submit(this::pollLoopSafely);
        }

        log.info("StockTaskConsumer started. node={}, pollWorkers={}", currentNodeId, pollWorkers);
    }

    /**
     * 关闭消费者：停止 poll loop，并尝试关闭 poll 线程池。
     * 注意：executePool 为复用线程池，通常不在此关闭（避免影响其他业务）。
     */
    @Override
    public void stop() {
        running.set(false);

        // 中断 poll worker，使其尽快退出。
        pollPool.shutdownNow();
        log.info("StockTaskConsumer stopped. node={}", currentNodeId);
    }

    @PreDestroy
    public void destroy() {
        // 容器关闭时保证 stop 被调用。
        stop();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Spring 生命周期阶段：越大越晚启动。
     * 放到最后启动，避免系统未就绪时开始拉任务。
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /* ===================== 核心：Poll Loop ===================== */

    /**
     * poll loop 包装：任何未捕获异常都要兜底，避免 worker 线程直接退出导致消费能力下降。
     */
    private void pollLoopSafely() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnceAndSubmit();
            } catch (InterruptedException ie) {
                // 响应中断：退出当前 worker。
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 兜底异常：记录并短暂退避，防止异常刷屏与空转。
                log.error("Poll loop unexpected error", e);
                sleepQuiet(300);
            }
        }
    }

    /**
     * 单次 poll + submit：
     * 1) 从队列拉取任务（可能为空）。
     * 2) 通过 submitLimiter 控制在飞任务数量，形成背压（poll 不会因 RejectedExecutionException 退化）。
     * 3) 将任务提交到 executePool 并执行 processWithRetry。
     */
    private void pollOnceAndSubmit() throws InterruptedException {
        // 从队列取一个任务（由 queueService 决定阻塞/非阻塞语义；当前代码假定可能返回 null）。
        StockRefreshTask task = queueService.poll();

        // 空队列：短暂 sleep，降低 CPU 空转。
        if (task == null) {
            TimeUnit.MILLISECONDS.sleep(EMPTY_QUEUE_SLEEP_MS);
            return;
        }

        // 背压控制：在飞任务已满时阻塞等待，避免疯狂 submit 造成 Rejected。
        submitLimiter.acquire();

        try {
            // 提交任务到执行线程池；提交失败需释放 permit。
            executePool.execute(() -> {
                try {
                    processWithRetry(task);
                } finally {
                    // 任务结束必须释放 permit，否则会导致系统“永久限流”。
                    submitLimiter.release();
                }
            });
        } catch (RejectedExecutionException ree) {
            // 理论上在背压控制下很少发生；发生时释放 permit 并记录日志。
            submitLimiter.release();
            log.warn("Execute pool rejected task. stockCode={}, traceId={}", task.getStockCode(), task.getTraceId(), ree);

            // 可选策略：这里不重入队列（按你的要求“内部重试”），直接标记失败。
            // 如果你希望“Rejected 也重试”，可以在这里做短暂 sleep 后再尝试一次提交。
        }
    }

    /* ===================== 核心：任务处理（内部重试） ===================== */

    /**
     * 任务处理入口：对“同一任务”做内部重试。
     * - 先拿锁（锁也做少量重试）
     * - 再执行 fetch/update（对可重试失败做重试）
     * - 最终释放锁、落库记录、清理状态
     */
    private void processWithRetry(StockRefreshTask task) {
        final String stockCode = task.getStockCode();
        final String traceId = task.getTraceId();

        // 基础校验：避免 NPE 与无效任务污染系统。
        if (isBlank(stockCode)) {
            log.warn("Skip invalid task: empty stockCode, traceId={}", traceId);
            return;
        }

        // 1) 分布式锁：避免同一 stockCode 被多节点/多线程并发刷新。
        boolean locked = tryLockWithRetry(stockCode);
        if (!locked) {
            // 锁拿不到：说明被其他节点占用（ACTIVE RUNNING in other node）。
            // 不做任何状态更新，避免覆盖 legitimate RUNNING status。
            return;
        }

        String dbStatus = "FAILED";
        String dbResult = "";
        String stockName = null;
        boolean executed = false;

        try {
            // 状态置为运行中：便于前端/运维查看。
            updateStatus(stockCode, StockTaskStatus.STATUS_RUNNING, null, traceId);

            // 2) 获取 Watchstock（用于拿 api、名称等）。
            Watchstock ws = watchstockService.getWatchStockByCode(stockCode);
            if (ws == null) {
                // 不可恢复：DB 没有该股票，重试无意义。
                dbResult = "Stock not found in DB";
                return;
            }
            stockName = ws.getName();

            // 3) 校验 URL：不可恢复，直接失败终态。
            String apiUrl = ws.getStockApi();
            if (apiUrl == null || apiUrl.contains("secid=null")) {
                dbResult = "INVALID_URL";
                return;
            }

            // 4) Fetch + Update：对“可恢复失败”做内部重试（HTTP 抖动、瞬时异常、返回 null）。
            StockRealtimeInfo info = fetchRealtimeWithRetry(apiUrl);
            if (info == null) {
                dbResult = "Fetch returned null after retry";
                return;
            }

            // 5) 更新实体（内存）并落库。
            watchStockUpdater.updateFromRealtimeInfo(ws, info);
            watchstockService.updateWatchstock(ws);
            asyncUpdateTradeRecords(ws.getCode(), ws.getNewPrice());
            queryStockProfitData();
            if (info.getPrice() != null) {
                dbStatus = "SUCCESS";
                dbResult = "Price=" + info.getPrice();
                executed = true;

                // 1. 获取当前最低价格和阈值
                BigDecimal currentPrice = BigDecimal.valueOf(info.getLowPrice());
                BigDecimal threshold = ws.getThresholdPrice();

                // 2. 增加判空逻辑：只有当 threshold 不为 null 时才进行比较
                if (threshold != null && currentPrice.compareTo(threshold) < 0) {
                    // 价格小于阈值触发提醒
                    sendNotification(task);
                }
            } else {
                dbStatus = "FAILED";
                dbResult = "Price= null";
            }

        } catch (Exception e) {
            // 未预期异常：记录并失败终态。
            log.error("Task failed: stockCode={}, traceId={}", stockCode, traceId, e);
            dbResult = Objects.toString(e.getMessage(), "Exception");
            dbStatus = "FAILED";
        } finally {
            // 6) 释放锁：务必放 finally，防止死锁。
            safeReleaseLock(stockCode);

            // 7) 记录执行结果：只记终态（SUCCESS/FAILED）。
            saveExecutionRecord(stockCode, stockName, dbStatus, dbResult, traceId);

            // 8) Atomic Cleanup: Immediate deletion for Monitor semantics.
            // (Only WAITING/RUNNING tasks appear in monitor)
            queueService.deleteStatus(stockCode, traceId);

            // 9) Decrement Batch Counter & Trigger Next Loop
            long remaining = queueService.decrementBatch(traceId);
            if (remaining == 0) {
                log.info("Batch completed (traceId={}). Triggering next batch...", traceId);
                stockWatchProcessor.triggerNextBatch();
            } else if (remaining < 0) {
                // Batch counter missing (expired or lost): Use recovery lock to trigger safely
                log.warn("Batch counter missing (remaining < 0). Attempting recovery... traceId={}", traceId);
                if (queueService.tryLockRecovery(traceId)) {
                    log.info("Recovery lock acquired. Triggering next batch... traceId={}", traceId);
                    stockWatchProcessor.triggerNextBatch();
                }
            }
        }
    }

    /**
     * 尝试获取分布式锁（短暂重试）。
     * - 目的：降低瞬时锁竞争导致的 SKIPPED。
     * - 边界：不要重试太久，否则会拖慢整体吞吐并占用执行线程。
     */
    private boolean tryLockWithRetry(String stockCode) {
        for (int i = 1; i <= LOCK_ATTEMPTS; i++) {
            // 尝试加锁：成功则返回。
            if (queueService.tryLockStock(stockCode, currentNodeId)) {
                return true;
            }
            // 失败短暂等待后再试。
            sleepQuiet(LOCK_RETRY_SLEEP_MS);
        }
        return false;
    }

    /**
     * 拉取实时数据（内部重试）。
     * 重试策略：
     * - 对异常、返回 null 视为“可恢复失败”，进行重试。
     * - 退避：线性递增（RETRY_BASE_DELAY_MS * attempt），避免对上游造成冲击。
     */
    private StockRealtimeInfo fetchRealtimeWithRetry(String apiUrl) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                StockRealtimeInfo info = KlineDataFetcher.fetchRealtimeInfo(apiUrl);
                if (info != null) {
                    return info;
                }
                // 返回 null：按可恢复失败处理，继续重试。
                log.warn("Fetch returned null, attempt={}/{}", attempt, MAX_ATTEMPTS);
            } catch (Exception e) {
                // 异常：按可恢复失败处理，继续重试。
                log.warn("Fetch exception, attempt={}/{}", attempt, MAX_ATTEMPTS, e);
            }

            // 最后一次不需要 sleep。
            if (attempt < MAX_ATTEMPTS) {
                sleepQuiet(RETRY_BASE_DELAY_MS * attempt);
            }
        }
        return null;
    }

    /**
     * 安全释放锁：避免释放异常导致 finally 中断（确保后续落库与清理执行）。
     */
    private void safeReleaseLock(String stockCode) {
        try {
            queueService.releaseLock(stockCode, currentNodeId);
        } catch (Exception e) {
            log.error("Release lock failed: stockCode={}", stockCode, e);
        }
    }

    /* ===================== 状态与记录 ===================== */

    /**
     * 更新任务状态到 Redis：
     * - status：RUNNING / SKIPPED 等
     * - lastResult：补充信息（例如锁占用、失败原因）
     */
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

    /**
     * 保存执行记录到数据库：
     * - 用于审计、排障、统计成功率与耗时等
     * - 失败时仅记录日志，不影响主流程（避免“记录失败导致业务失败”）
     */
    private void saveExecutionRecord(String stockCode, String stockName, String status, String result, String traceId) {
        try {
            StockRefreshExecuteRecord record = new StockRefreshExecuteRecord();
            record.setStockCode(stockCode);
            record.setStockName(stockName);
            record.setStatus(status);
            record.setExecuteResult(result);
            record.setNodeIp(currentNodeId);
            record.setExecuteTime(new Date());
            record.setTraceId(traceId);
            recordService.insertStockRefreshExecuteRecord(record);
        } catch (Exception e) {
            log.error("Failed to save execution record. stockCode={}, traceId={}", stockCode, traceId, e);
        }
    }

    /* ===================== 工具方法 ===================== */

    /**
     * 线程友好 sleep：吞掉 InterruptedException 的同时保留中断标记。
     */
    private void sleepQuiet(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 判空工具：避免引入额外依赖。
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 发送通知并记录日志
     */
    private boolean sendNotification(StockListingNotice existing, String message) {
        try {
            SendEmail.notification(JSON.toJSONString(existing), existing.getSecurityName() + message);
            log.info("发送通知成功：{}，股票：{}", message, existing.getSecurityCode());
            return true;
        } catch (Exception e) {
            log.error("发送通知失败：股票：{}，原因：{}", existing.getSecurityCode(), e.getMessage(), e);
            return false;
        }
    }

    private void sendNotification(StockRefreshTask task) {
        try {
            Watchstock ws = watchstockService.getWatchStockByCode(task.getStockCode());
            if (ws == null || ws.getNum() >= 3) return;
            ws.setNum(ws.getNum() + 1);
            watchstockService.updateWatchstock(ws);
            StockListingNotice notice = new StockListingNotice();
            notice.setSecurityCode(ws.getCode());
            notice.setSecurityName(ws.getName());
            notice.setCurrentPrice(ws.getNewPrice());
            sendNotification(notice, "价格已突破提醒价位");
        } catch (Exception e) {
            log.error("Failed to send price alert for {}", task.getStockCode(), e);
        }
    }

    /**
     * 异步更新指定股票代码的交易记录及相关卖出价格预警信息
     *
     * @param code     股票代码
     * @param newPrice 最新价格，用于更新交易记录和预警价格
     */
    private void asyncUpdateTradeRecords(String code, BigDecimal newPrice) {
        CompletableFuture.runAsync(() -> {
            try {
                // 查询指定股票代码的交易记录
                List<StockTrades> tradesList = stockTradesService.selectStockTradesOne(
                        new StockTrades().setStockCode(code));
                for (StockTrades stockTrades : tradesList) {
                    if (stockTrades != null && stockTrades.getStockCode().equals(code)) {
                        // 计算更新交易明细
                        updateTradeDetails(stockTrades, newPrice);

                        // 更新数据库中的交易记录
                        stockTradesService.updateStockTradesByCode(stockTrades);

                        // 更新卖出价格预警中的最新价格
                        SellPriceAlerts sellPriceAlerts = new SellPriceAlerts()
                                .setStockCode(stockTrades.getStockCode())
                                .setLatestPrice(newPrice);
                        sellPriceAlertsService.updateLatestPrice(sellPriceAlerts);
                    }
                }

            } catch (Exception e) {
                log.error("更新交易记录失败: {}", code, e);
            }
        }, ThreadPoolUtil.getCoreExecutor());
    }

    /**
     * 计算并更新交易详情
     * Calculate and update trade details
     */
    private void updateTradeDetails(StockTrades trade, BigDecimal newPrice) {
        try {
            BigDecimal[] addPrices = {
                    trade.getAdditionalPrice1(),
                    trade.getAdditionalPrice2(),
                    trade.getAdditionalPrice3()
            };

            Long[] addShares = {
                    trade.getAdditionalShares1(),
                    trade.getAdditionalShares2(),
                    trade.getAdditionalShares3()
            };

            BigDecimal profit = calculateNetProfit(
                    trade.getBuyPrice(),
                    newPrice,
                    trade.getInitialShares(),
                    addPrices,
                    addShares
            );

            BigDecimal targetNetProfit = calculateNetProfit(
                    trade.getBuyPrice(),
                    trade.getSellTargetPrice(),
                    trade.getInitialShares(),
                    addPrices,
                    addShares
            );

            if (trade.getSellTargetPrice().equals(newPrice)) {
                trade.setIsSell(1);
            }

            trade.setSellPrice(newPrice)
                    .setNetProfit(profit)
                    .setTargetNetProfit(targetNetProfit)
                    .setTotalCost(calculateTotalCost(
                            trade.getBuyPrice(),
                            trade.getInitialShares(),
                            addPrices,
                            addShares
                    ));
        } catch (Exception e) {
            log.error("计算交易详情失败:", e);
        }

    }

    /**
     * 计算净收益
     * Calculate net profit
     */
    private BigDecimal calculateNetProfit(BigDecimal buyPrice, BigDecimal sellPrice,
                                          Long initShares, BigDecimal[] addPrices, Long[] addShares) {
        BigDecimal baseProfit = sellPrice.subtract(buyPrice)
                .multiply(new BigDecimal(initShares));

        BigDecimal additionalProfit = IntStream.range(0, Math.min(addPrices.length, addShares.length))
                .filter(i -> addPrices[i] != null && addShares[i] != null)
                .mapToObj(i -> sellPrice.subtract(addPrices[i])
                        .multiply(new BigDecimal(addShares[i])))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseProfit.add(additionalProfit);
    }

    /**
     * 计算总成本
     * Calculate total cost
     */
    private BigDecimal calculateTotalCost(BigDecimal buyPrice, Long initShares,
                                          BigDecimal[] addPrices, Long[] addShares) {
        BigDecimal baseCost = buyPrice.multiply(new BigDecimal(initShares));

        BigDecimal additionalCost = IntStream.range(0, Math.min(addPrices.length, addShares.length))
                .filter(i -> addPrices[i] != null && addShares[i] != null)
                .mapToObj(i -> addPrices[i].multiply(new BigDecimal(addShares[i])))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseCost.add(additionalCost);
    }

    /**
     * 查询当日所有交易记录，按用户分组累加净利润，
     * 并将每个用户的净利润写入或更新 SalesData 表中的当天数据。
     *
     * @throws RuntimeException 如果在日期转换或数据库操作中发生不可恢复错误
     */
    public void queryStockProfitData() {
        // 1. 查询所有交易记录
        List<StockTrades> trades = stockTradesService.selectStockTradesList(new StockTrades());
        if (CollectionUtils.isEmpty(trades)) {
            log.info("【StockProfitService】未查询到任何交易记录，跳过当天净利润统计");
            return;
        }

        // 2. 按 userId 分组，计算每个用户的总净利润
        Map<Long, BigDecimal> profitByUser = trades.stream()
                .filter(t -> t.getNetProfit() != null && t.getUserId() != null) // 过滤无效记录
                .collect(Collectors.groupingBy(
                        StockTrades::getUserId,                                     // 以 userId 分组
                        Collectors.mapping(                                        // 提取净利润字段
                                StockTrades::getNetProfit,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)   // 累加
                        )
                ));

        // 3. 获取当日零点日期，用于 SalesData.recordDate
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Date recordDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 4. 针对每个用户，写入或更新 SalesData
        profitByUser.forEach((userId, totalProfit) -> {
            SalesData criteria = new SalesData();
            criteria.setRecordDate(recordDate);
            criteria.setUserId(userId);

            // 4.1 查询是否已存在当日记录
            List<SalesData> existing = salesDataService.selectSalesDataList(criteria);
            SalesData salesData = new SalesData();
            salesData.setRecordDate(recordDate);
            salesData.setUserId(userId);
            salesData.setProfit(totalProfit);

            if (CollectionUtils.isNotEmpty(existing)) {
                // 4.2 如果存在则更新
                salesData.setId(existing.get(0).getId());
                salesDataService.updateSalesData(salesData);
                log.info("【StockProfitService】已更新用户 {} 在 {} 的净利润，金额：{}",
                        userId, today.format(DATE_FORMATTER), totalProfit);
            } else {
                // 4.3 如果不存在则插入
                salesDataService.insertSalesData(salesData);
                log.info("【StockProfitService】已插入用户 {} 在 {} 的净利润，金额：{}",
                        userId, today.format(DATE_FORMATTER), totalProfit);
            }
        });

        // 5. 异步分别触发每个用户的年度投资汇总更新
        profitByUser.forEach((userId, totalProfit) -> {
            ThreadPoolUtil.getCoreExecutor().submit(() -> {
                // 传入单个用户净利润，按业务逻辑更新该用户的年度汇总
                queryAndUpdateYearlyInvestmentSummary(totalProfit, userId);
            });
        });
    }


    /**
     * 查询并更新当年投资汇总数据。
     *
     * <p>该方法执行以下步骤：
     * <ol>
     *   <li>根据当前年份构造查询条件，调用 service 查询当年投资汇总列表；</li>
     *   <li>若列表不为空，取第一条记录并执行更新操作；</li>
     *   <li>若列表为空，则记录警告日志；</li>
     *   <li>捕获并记录执行过程中的异常。</li>
     * </ol>
     *
     * @param totalProfit 本年度累计利润（单位：元），用于更新汇总记录中的利润字段
     */
    public void queryAndUpdateYearlyInvestmentSummary(BigDecimal totalProfit, Long userId) {
        // 动态获取当前年份
        long currentYear = Year.now().getValue();
        try {
            // 构造查询条件：查询当前年份的汇总记录
            YearlyInvestmentSummary queryCondition = new YearlyInvestmentSummary();
            queryCondition.setYear(currentYear);
            queryCondition.setUserId(userId);

            List<YearlyInvestmentSummary> summaries =
                    yearlyInvestmentSummaryService.selectYearlyInvestmentSummaryList(queryCondition);

            if (summaries == null || summaries.isEmpty()) {
                // 如果无数据，记录警告并返回
                log.warn("queryAndUpdateYearlyInvestmentSummary：未查询到 {} 年的投资汇总记录", currentYear);
                return;
            }

            // 取第一条记录进行更新
            YearlyInvestmentSummary summaryToUpdate = summaries.get(0);

            BigDecimal startPrincipal = summaryToUpdate.getStartPrincipal() != null
                    ? summaryToUpdate.getStartPrincipal()
                    : BigDecimal.ZERO;

            BigDecimal profit = totalProfit != null
                    ? totalProfit
                    : BigDecimal.ZERO;

            BigDecimal actualEndValue = profit.add(startPrincipal);
            summaryToUpdate.setActualEndValue(actualEndValue);

            yearlyInvestmentSummaryService.updateYearlyInvestmentSummary(summaryToUpdate);
            log.info("成功更新 {} 年度投资汇总，累计利润：{}", currentYear, totalProfit);
        } catch (Exception e) {
            // 捕获并输出完整异常信息，便于排查
            log.error("queryAndUpdateYearlyInvestmentSummary 更新失败，年份：{}，累计利润：{}",
                    currentYear, totalProfit, e);
        }
    }
}
