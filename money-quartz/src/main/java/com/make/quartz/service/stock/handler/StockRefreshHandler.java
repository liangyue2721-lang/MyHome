package com.make.quartz.service.stock.handler;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.ThreadPoolUtil;
import com.make.finance.domain.YearlyInvestmentSummary;
import com.make.finance.service.IYearlyInvestmentSummaryService;
import com.make.quartz.domain.StockRefreshExecuteRecord;
import com.make.quartz.domain.StockRefreshTask;
import com.make.quartz.domain.StockTaskStatus;
import com.make.quartz.service.IStockRefreshExecuteRecordService;
import com.make.quartz.service.impl.WatchStockUpdater;
import com.make.quartz.service.stock.queue.StockTaskQueueService;
import com.make.quartz.util.DateUtil;
import com.make.quartz.util.email.SendEmail;
import com.make.stock.domain.*;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.ISalesDataService;
import com.make.stock.service.ISellPriceAlertsService;
import com.make.stock.service.IStockTradesService;
import com.make.stock.service.IWatchstockService;
import com.make.stock.util.KlineDataFetcher;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 股票刷新业务处理器实现类
 * <p>
 * 将原 Consumer 中的业务逻辑抽离至此，包括：
 * 1. Fetch 数据
 * 2. Update WatchStock
 * 3. Update Trade Records
 * 4. Calculate Profits (Optimized)
 * 5. Notifications
 * 6. DB Logging
 */
@Service
public class StockRefreshHandler implements IStockRefreshHandler {

    private static final Logger log = LoggerFactory.getLogger(StockRefreshHandler.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 300;

    @Resource
    private IWatchstockService watchstockService;
    @Resource
    private WatchStockUpdater watchStockUpdater;
    @Resource
    private IStockTradesService stockTradesService;
    @Resource
    private ISellPriceAlertsService sellPriceAlertsService;
    @Resource
    private ISalesDataService salesDataService;
    @Resource
    private IYearlyInvestmentSummaryService yearlyInvestmentSummaryService;
    @Resource
    private IStockRefreshExecuteRecordService recordService;
    @Resource
    private StockTaskQueueService queueService;

    @Override
    public void refreshStock(StockRefreshTask task) {
        String stockCode = task.getStockCode();
        String traceId = task.getTraceId();

        String dbStatus = "FAILED";
        String dbResult = "";
        String stockName = null;

        try {
            // 1. Update Status to RUNNING
            updateStatus(stockCode, StockTaskStatus.STATUS_RUNNING, null, traceId);

            // 2. Get WatchStock
            Watchstock ws = watchstockService.getWatchStockByCode(stockCode);
            if (ws == null) {
                dbResult = "Stock not found in DB";
                return;
            }
            stockName = ws.getName();

            // 3. Check URL
            String apiUrl = ws.getStockApi();
            if (apiUrl == null || apiUrl.contains("secid=null")) {
                dbResult = "INVALID_URL";
                return;
            }

            // 4. Fetch Realtime Data (Retry Logic)
            StockRealtimeInfo info = fetchRealtimeWithRetry(apiUrl);
            if (info == null) {
                dbResult = "Fetch returned null after retry";
                return;
            }

            // 5. Update WatchStock
            watchStockUpdater.updateFromRealtimeInfo(ws, info);
            watchstockService.updateWatchstock(ws);

            // 6. Update Trade Records (Synchronous now to avoid race conditions)
            updateTradeRecordsSync(ws.getCode(), ws.getNewPrice());

            // 7. Calculate Profits (Optimized & Synchronous)
            if (DateUtil.isValidWorkday()) {
                updateProfitForHolders(ws.getCode());
            }

            // 8. Notifications
            if (info.getPrice() != null) {
                dbStatus = "SUCCESS";
                dbResult = "Price=" + info.getPrice();

                BigDecimal currentPrice = BigDecimal.valueOf(info.getLowPrice());
                BigDecimal threshold = ws.getThresholdPrice();
                if (threshold != null && currentPrice.compareTo(threshold) < 0) {
                    sendNotification(task, ws);
                }
            } else {
                dbResult = "Price= null";
            }

        } catch (Exception e) {
            log.error("Task failed: stockCode={}, traceId={}", stockCode, traceId, e);
            dbResult = Objects.toString(e.getMessage(), "Exception");
        } finally {
            // 9. Save Execution Record
            saveExecutionRecord(stockCode, stockName, dbStatus, dbResult, traceId);
        }
    }

    private StockRealtimeInfo fetchRealtimeWithRetry(String apiUrl) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                StockRealtimeInfo info = KlineDataFetcher.fetchRealtimeInfo(apiUrl);
                if (info != null) {
                    return info;
                }
                log.warn("Fetch returned null, attempt={}/{}", attempt, MAX_ATTEMPTS);
            } catch (Exception e) {
                log.warn("Fetch exception, attempt={}/{}", attempt, MAX_ATTEMPTS, e);
            }

            if (attempt < MAX_ATTEMPTS) {
                sleepQuiet(RETRY_BASE_DELAY_MS * attempt);
            }
        }
        return null;
    }

    /**
     * 同步更新交易记录
     */
    private void updateTradeRecordsSync(String code, BigDecimal newPrice) {
        try {
            List<StockTrades> tradesList = stockTradesService.selectStockTradesOne(
                    new StockTrades().setStockCode(code));

            for (StockTrades stockTrades : tradesList) {
                if (stockTrades != null && stockTrades.getStockCode().equals(code)) {
                    updateTradeDetails(stockTrades, newPrice);
                    stockTradesService.updateStockTradesByCode(stockTrades);

                    SellPriceAlerts sellPriceAlerts = new SellPriceAlerts()
                            .setStockCode(stockTrades.getStockCode())
                            .setLatestPrice(newPrice);
                    sellPriceAlertsService.updateLatestPrice(sellPriceAlerts);
                }
            }
        } catch (Exception e) {
            log.error("更新交易记录失败: {}", code, e);
        }
    }

    /**
     * 优化后的利润更新逻辑：仅更新持有该股票的用户
     */
    private void updateProfitForHolders(String stockCode) {
        try {
            // 1. 找出持有该股票的所有用户 (利用 selectStockTradesList)
            List<StockTrades> holders = stockTradesService.selectStockTradesList(
                    new StockTrades().setStockCode(stockCode));

            if (CollectionUtils.isEmpty(holders)) {
                return;
            }

            List<Long> userIds = holders.stream()
                    .map(StockTrades::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // 2. 针对每个受影响的用户，重新计算当日总利润
            for (Long userId : userIds) {
                updateDailyProfitForUser(userId);
            }
        } catch (Exception e) {
            log.error("Failed to update profit for holders of {}", stockCode, e);
        }
    }

    private void updateDailyProfitForUser(Long userId) {
        // 获取该用户的所有持仓
        StockTrades query = new StockTrades();
        query.setUserId(userId);
        List<StockTrades> userTrades = stockTradesService.selectStockTradesList(query);

        // 累加净利润
        BigDecimal totalProfit = userTrades.stream()
                .filter(t -> t.getNetProfit() != null)
                .map(StockTrades::getNetProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 更新 SalesData
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Date recordDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        SalesData criteria = new SalesData();
        criteria.setRecordDate(recordDate);
        criteria.setUserId(userId);

        List<SalesData> existing = salesDataService.selectSalesDataList(criteria);
        SalesData salesData = new SalesData();
        salesData.setRecordDate(recordDate);
        salesData.setUserId(userId);
        salesData.setProfit(totalProfit);

        if (CollectionUtils.isNotEmpty(existing)) {
            salesData.setId(existing.get(0).getId());
            salesDataService.updateSalesData(salesData);
        } else {
            salesDataService.insertSalesData(salesData);
        }

        // 异步更新年度汇总 (这步比较轻量，可以异步，也可以同步)
        // 为保持一致性，这里改为同步或者提交到线程池
        // 原逻辑是提交到 coreExecutor，这里保持提交到线程池，避免阻塞太久
        ThreadPoolUtil.getCoreExecutor().submit(() -> {
            queryAndUpdateYearlyInvestmentSummary(totalProfit, userId);
        });
    }

    public void queryAndUpdateYearlyInvestmentSummary(BigDecimal totalProfit, Long userId) {
        long currentYear = Year.now().getValue();
        try {
            YearlyInvestmentSummary queryCondition = new YearlyInvestmentSummary();
            queryCondition.setYear(currentYear);
            queryCondition.setUserId(userId);

            List<YearlyInvestmentSummary> summaries =
                    yearlyInvestmentSummaryService.selectYearlyInvestmentSummaryList(queryCondition);

            if (summaries == null || summaries.isEmpty()) {
                return;
            }

            YearlyInvestmentSummary summaryToUpdate = summaries.get(0);
            BigDecimal startPrincipal = summaryToUpdate.getStartPrincipal() != null
                    ? summaryToUpdate.getStartPrincipal()
                    : BigDecimal.ZERO;

            BigDecimal profit = totalProfit != null ? totalProfit : BigDecimal.ZERO;
            BigDecimal actualEndValue = profit.add(startPrincipal);

            summaryToUpdate.setActualEndValue(actualEndValue);
            yearlyInvestmentSummaryService.updateYearlyInvestmentSummary(summaryToUpdate);

        } catch (Exception e) {
            log.error("queryAndUpdateYearlyInvestmentSummary failed for user {}", userId, e);
        }
    }

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

    private BigDecimal calculateNetProfit(BigDecimal buyPrice, BigDecimal sellPrice,
                                          Long initShares, BigDecimal[] addPrices, Long[] addShares) {
        if (buyPrice == null || sellPrice == null || initShares == null) return BigDecimal.ZERO;

        BigDecimal baseProfit = sellPrice.subtract(buyPrice)
                .multiply(new BigDecimal(initShares));

        BigDecimal additionalProfit = IntStream.range(0, Math.min(addPrices.length, addShares.length))
                .filter(i -> addPrices[i] != null && addShares[i] != null)
                .mapToObj(i -> sellPrice.subtract(addPrices[i])
                        .multiply(new BigDecimal(addShares[i])))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseProfit.add(additionalProfit);
    }

    private BigDecimal calculateTotalCost(BigDecimal buyPrice, Long initShares,
                                          BigDecimal[] addPrices, Long[] addShares) {
        if (buyPrice == null || initShares == null) return BigDecimal.ZERO;

        BigDecimal baseCost = buyPrice.multiply(new BigDecimal(initShares));

        BigDecimal additionalCost = IntStream.range(0, Math.min(addPrices.length, addShares.length))
                .filter(i -> addPrices[i] != null && addShares[i] != null)
                .mapToObj(i -> addPrices[i].multiply(new BigDecimal(addShares[i])))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseCost.add(additionalCost);
    }

    private void sendNotification(StockRefreshTask task, Watchstock ws) {
        try {
            if (ws == null || ws.getNum() >= 3) return;

            ws.setNum(ws.getNum() + 1);
            watchstockService.updateWatchstock(ws);

            StockListingNotice notice = new StockListingNotice();
            notice.setSecurityCode(ws.getCode());
            notice.setSecurityName(ws.getName());
            notice.setCurrentPrice(ws.getNewPrice());

            String message = "价格已突破提醒价位";
            SendEmail.notification(JSON.toJSONString(notice), notice.getSecurityName() + message);
        } catch (Exception e) {
            log.error("Failed to send price alert for {}", task.getStockCode(), e);
        }
    }

    private void updateStatus(String stockCode, String status, String result, String traceId) {
        StockTaskStatus s = new StockTaskStatus();
        s.setStockCode(stockCode);
        s.setStatus(status);
        s.setOccupiedByNode(null); // Assuming node ID is handled by queue service if needed, or pass it?
        // Actually StockTaskConsumer sets occupiedByNode = currentNodeId.
        // Handler doesn't know currentNodeId easily unless passed or fetched.
        // For now, let's skip occupiedByNode here or IpUtils.getHostIp()
        // But updating status to RUNNING usually refreshes the lock info too.
        // Let's assume Consumer handles lock ownership and this is just status display.
        s.setOccupiedTime(System.currentTimeMillis());
        s.setTraceId(traceId);
        s.setLastResult(result);
        queueService.updateStatus(stockCode, s);
    }

    private void saveExecutionRecord(String stockCode, String stockName, String status, String result, String traceId) {
        try {
            StockRefreshExecuteRecord record = new StockRefreshExecuteRecord();
            record.setStockCode(stockCode);
            record.setStockName(stockName);
            record.setStatus(status);
            record.setExecuteResult(result);
            // record.setNodeIp(currentNodeId); // Handler might need node IP
            // Let's use IpUtils here or leave it null?
            // The original used currentNodeId field.
            record.setNodeIp(com.make.common.utils.ip.IpUtils.getHostIp());
            record.setExecuteTime(new Date());
            record.setTraceId(traceId);
            recordService.insertStockRefreshExecuteRecord(record);
        } catch (Exception e) {
            log.error("Failed to save execution record. stockCode={}, traceId={}", stockCode, traceId, e);
        }
    }

    private void sleepQuiet(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
