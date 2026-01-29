package com.make.stock.service.scheduled.stock.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.make.common.utils.ThreadPoolUtil;
import com.make.finance.domain.YearlyInvestmentSummary;
import com.make.finance.service.IYearlyInvestmentSummaryService;
import com.make.stock.domain.StockRefreshExecuteRecord;
import com.make.stock.domain.StockRefreshTask;
import com.make.stock.domain.StockTaskStatus;
import com.make.stock.service.scheduled.IStockRefreshExecuteRecordService;
import com.make.stock.service.scheduled.impl.WatchStockUpdater;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import com.make.stock.util.DateUtil;
import com.make.stock.util.email.SendEmail;
import com.make.stock.domain.*;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.ISalesDataService;
import com.make.stock.service.ISellPriceAlertsService;
import com.make.stock.service.IStockTradesService;
import com.make.stock.service.IWatchstockService;
import com.make.stock.util.KlineDataFetcher;
import com.make.common.annotation.IdempotentConsumer;
import com.make.stock.mapper.StockKlineTaskMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * 股票刷新业务处理器实现类
 * <p>
 * 将原 Consumer 中的业务逻辑抽离至此，实现业务与调度分离。包含：
 * 1. Fetch 数据（带重试）
 * 2. Update WatchStock（更新价格）
 * 3. Update Trade Records（更新交易记录与预警）
 * 4. Calculate Profits (优化后的按用户计算)
 * 5. Notifications（价格预警通知）
 * 6. DB Logging（执行日志落库）
 */
@Service
public class StockRefreshHandler implements IStockRefreshHandler {

    private static final Logger log = LoggerFactory.getLogger(StockRefreshHandler.class);

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
    @Resource
    private StockKlineTaskMapper stockKlineTaskMapper;

    /**
     * 处理股票刷新任务的主入口
     *
     * @param task 任务信息对象，包含 stockCode 和 traceId
     */
    @Override
    @IdempotentConsumer(key = "#task.traceId + '-' + #task.stockCode", expire = 7200)
    public void refreshStock(StockRefreshTask task) {
        String stockCode = task.getStockCode();
        String traceId = task.getTraceId();

        String dbStatus = "FAILED";
        String dbResult = "";
        String stockName = null;

        try {
            // 1. 更新 Redis 状态为 RUNNING
            updateStatus(stockCode, StockTaskStatus.STATUS_RUNNING, null, traceId);

            // 2. 获取关注股票信息
            Watchstock ws = watchstockService.getWatchStockByCode(stockCode);
            if (ws == null) {
                dbResult = "Stock not found in DB";
                return;
            }
            stockName = ws.getName();

            StockRealtimeInfo info = null;

            // 尝试从 K 线服务获取数据（优先策略）
            String market = getMarketFromTask(stockCode);
            if (market != null && !market.isEmpty()) {
                info = null;
                // toDO 存在并发问题以及丢失数据问题，需要重新设计并发控制策略
//                info = fetchFromKlineData(stockCode, market);
            }

            // Fallback: 如果 K 线数据获取失败，使用原有 URL 方式
            if (info == null) {
                // 3. 校验 API URL
                String apiUrl = ws.getStockApi();
                if (apiUrl == null || apiUrl.contains("secid=null")) {
                    dbResult = "INVALID_URL";
                    return;
                }

                // 4. 获取实时数据（带重试机制）
                info = fetchRealtimeWithRetry(apiUrl);
            }

            if (info == null) {
                dbResult = "Fetch returned null after retry";
                return;
            }

            if (ws.getLowPrice() != null && ws.getThresholdPrice() != null) {
                BigDecimal currentPrice = ws.getLowPrice();
                BigDecimal threshold = ws.getThresholdPrice();
                if (threshold != null && currentPrice.compareTo(threshold) < 0) {
                    sendNotification(task, ws);
                }
            }

            // 5. 更新 WatchStock 实体与数据库
            watchStockUpdater.updateFromRealtimeInfo(ws, info);
            watchstockService.updateWatchstock(ws);

            // 6. 同步更新交易记录（解决并发数据不一致问题）
            updateTradeRecordsSync(ws.getCode(), ws.getNewPrice());

            // 7. 计算利润（仅在工作日执行）
            if (DateUtil.isValidWorkday()) {
                updateProfitForHolders(ws.getCode());
            }

            // 8. 检查并发送价格预警通知
            if (info.getPrice() != null) {
                dbStatus = "SUCCESS";
                dbResult = "Price=" + info.getPrice();

            } else {
                dbResult = "Price= null";
            }

            if (ws.getLowPrice() != null && ws.getThresholdPrice() != null) {
                BigDecimal currentPrice = ws.getLowPrice();
                BigDecimal threshold = ws.getThresholdPrice();
                if (threshold != null && currentPrice.compareTo(threshold) < 0) {
                    sendNotification(task, ws);
                }
            }

        } catch (Exception e) {
            log.error("Task failed: stockCode={}, traceId={}", stockCode, traceId, e);
            dbResult = Objects.toString(e.getMessage(), "Exception");
        } finally {
            // 9. 保存执行记录
            saveExecutionRecord(stockCode, stockName, dbStatus, dbResult, traceId);
        }
    }

    /**
     * 尝试查询 stock_kline_task 表获取 market
     */
    private String getMarketFromTask(String stockCode) {
        try {
            StockKlineTask query = new StockKlineTask();
            query.setStockCode(stockCode);
            List<StockKlineTask> tasks = stockKlineTaskMapper.selectStockKlineTaskList(query);
            if (CollectionUtils.isNotEmpty(tasks)) {
                return tasks.get(0).getMarket();
            }
        } catch (Exception e) {
            log.warn("Failed to query market from stock_kline_task for {}", stockCode, e);
        }
        return null;
    }

    /**
     * 通过 KlineDataFetcher 获取 5 日 K 线并取最新一条转换为 RealtimeInfo
     */
    private StockRealtimeInfo fetchFromKlineData(String stockCode, String market) {
        try {
            List<KlineData> klineDataList = KlineDataFetcher.fetchKlineDataFiveDay(stockCode, market);
            if (CollectionUtils.isNotEmpty(klineDataList)) {
                // 取最后一条（最新日期）
                KlineData latest = klineDataList.get(klineDataList.size() - 1);
                return convertKlineToRealtime(stockCode, latest);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch kline data for code={}, market={}", stockCode, market, e);
        }
        return null;
    }

    /**
     * 将 KlineData 转换为 StockRealtimeInfo
     */
    private StockRealtimeInfo convertKlineToRealtime(String stockCode, KlineData kline) {
        StockRealtimeInfo info = new StockRealtimeInfo();
        info.setStockCode(stockCode);
        info.setPrice(kline.getClose());
        info.setPrevClose(kline.getPreClose());
        info.setOpenPrice(kline.getOpen());
        info.setHighPrice(kline.getHigh());
        info.setLowPrice(kline.getLow());
        info.setVolume(kline.getVolume());
        info.setTurnover(kline.getAmount());
        return info;
    }

    /**
     * 获取实时数据（带内部重试）
     *
     * @param apiUrl 股票 API 接口地址
     * @return 实时数据对象 StockRealtimeInfo，若重试多次仍失败则返回 null
     */
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
     * 同步更新交易记录及预警价格
     *
     * @param code     股票代码
     * @param newPrice 最新价格
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
     *
     * @param stockCode 股票代码
     */
    private void updateProfitForHolders(String stockCode) {
        try {
            // 1. 找出持有该股票的所有用户 (利用 selectStockTradesList)
            // 在 updateProfitForHolders 方法中替换硬编码的年份
            long currentYear = Year.now().getValue();
            List<StockTrades> holders = stockTradesService.selectStockTradesByYear((int) currentYear);

            if (CollectionUtils.isEmpty(holders)) {
                return;
            }

            // 提取去重的 userId 列表
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

    /**
     * 更新指定用户的当日总利润及年度汇总
     *
     * @param userId 用户ID
     */
    private void updateDailyProfitForUser(Long userId) {
        // 获取该用户的所有持仓
        List<StockTrades> userTrades = stockTradesService.selectStockTradesByYearAndUserId(Year.now().getValue(), userId);

        // 累加净利润
        BigDecimal totalProfit = userTrades.stream()
                .filter(t -> t.getNetProfit() != null)
                .map(StockTrades::getNetProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 更新 SalesData（日报表）
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

        // 异步更新年度汇总（防止阻塞主流程）
        ThreadPoolUtil.getCoreExecutor().submit(() -> {
            queryAndUpdateYearlyInvestmentSummary(totalProfit, userId);
        });
    }

    /**
     * 查询并更新当年投资汇总数据
     *
     * @param totalProfit 本年度累计利润
     * @param userId      用户ID
     */
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

    /**
     * 计算并更新交易详情（利润、成本、目标达成等）
     *
     * @param trade    交易记录实体
     * @param newPrice 最新股票价格
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

            if (trade.getSellTargetPrice() != null && trade.getSellTargetPrice().equals(newPrice)) {
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
     *
     * @param buyPrice   买入价格
     * @param sellPrice  卖出/当前价格
     * @param initShares 初始持仓数量
     * @param addPrices  追加买入价格数组
     * @param addShares  追加买入数量数组
     * @return 总净收益 (基础收益 + 追加部分收益)
     */
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

    /**
     * 计算总成本
     *
     * @param buyPrice   初始买入价格
     * @param initShares 初始持仓数量
     * @param addPrices  追加买入价格数组
     * @param addShares  追加买入数量数组
     * @return 总投入成本
     */
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

    /**
     * 发送价格预警通知
     *
     * @param task 当前任务信息
     * @param ws   关注股票信息
     */
    private void sendNotification(StockRefreshTask task, Watchstock ws) {
        try {
            if (ws == null || ws.getNum() >= 3) return;

            ws.setNum(ws.getNum() + 1);
            watchstockService.updateWatchstock(ws);

            Watchstock notice = new Watchstock();
            notice.setCode(ws.getCode());
            notice.setName(ws.getName());
            notice.setNewPrice(ws.getNewPrice());

            // 1. 标题优化：增加【】标识和核心信息，方便在收件箱快速扫视
            String subject = String.format("【价格预警】%s (%s) 已达到预设目标价",
                    ws.getName(), ws.getCode());

            // 2. 构造美化后的 HTML 邮件模板
            String htmlContent = String.format(
                    "<div style='font-family: \"Microsoft YaHei\", -apple-system, sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);'>" +
                            "  " +
                            "  <div style='background: linear-gradient(90deg, #e11d48 0%%, #fb7185 100%%); padding: 20px; text-align: center;'>" +
                            "    <h2 style='margin: 0; color: white; font-size: 20px; letter-spacing: 1px;'>📈 实时价格预警触发</h2>" +
                            "  </div>" +
                            "  " +
                            "  <div style='padding: 30px; background-color: #ffffff;'>" +
                            "    <p style='margin-top: 0; color: #4b5563; font-size: 15px;'>您好，系统检测到您关注的个股行情已触发预设条件：</p>" +
                            "    " +
                            "    " +
                            "    <table style='width: 100%%; border-collapse: collapse; margin: 25px 0; background-color: #fffafb; border-radius: 8px;'>" +
                            "      <tr>" +
                            "        <td style='padding: 12px 15px; color: #6b7280; font-size: 14px; border-bottom: 1px solid #fee2e2;'>股票信息</td>" +
                            "        <td style='padding: 12px 15px; font-weight: bold; color: #111827; border-bottom: 1px solid #fee2e2;'>%s (%s)</td>" +
                            "      </tr>" +
                            "      <tr>" +
                            "        <td style='padding: 12px 15px; color: #6b7280; font-size: 14px; border-bottom: 1px solid #fee2e2;'>当前价格</td>" +
                            "        <td style='padding: 12px 15px; font-size: 26px; font-weight: 800; color: #e11d48; border-bottom: 1px solid #fee2e2;'>%s</td>" +
                            "      </tr>" +
                            "      <tr>" +
                            "        <td style='padding: 12px 15px; color: #6b7280; font-size: 14px; border-bottom: 1px solid #fee2e2;'>预警门槛</td>" +
                            "        <td style='padding: 12px 15px; font-weight: 600; color: #374151; border-bottom: 1px solid #fee2e2;'>%s</td>" +
                            "      </tr>" +
                            "      <tr>" +
                            "        <td style='padding: 12px 15px; color: #6b7280; font-size: 14px;'>触发时间</td>" +
                            "        <td style='padding: 12px 15px; color: #6b7280; font-size: 14px;'>%s</td>" +
                            "      </tr>" +
                            "    </table>" +
                            "    " +
                            "    " +
                            "    <div style='margin-top: 30px; border-top: 1px solid #f3f4f6; padding-top: 20px;'>" +
                            "      <p style='color: #9ca3af; font-size: 12px; margin-bottom: 10px; font-weight: bold;'>DEBUG INFO / 原始数据回执：</p>" +
                            "      <pre style='background: #1f2937; color: #34d399; padding: 15px; border-radius: 6px; font-size: 12px; overflow-x: auto; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; line-height: 1.5;'>%s</pre>" +
                            "    </div>" +
                            "  </div>" +
                            "  " +
                            "  <div style='background-color: #f9fafb; padding: 15px; text-align: center;'>" +
                            "    <p style='color: #9ca3af; font-size: 12px; margin: 0;'>本邮件由量化预警系统自动发送，请勿回复。</p>" +
                            "  </div>" +
                            "</div>",
                    ws.getName(), ws.getCode(),
                    ws.getNewPrice(),
                    ws.getThresholdPrice(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    JSON.toJSONString(notice, JSONWriter.Feature.PrettyFormat)
            );

            SendEmail.sendHtml(htmlContent, subject, "lyp0028nxyf@163.com");

        } catch (Exception e) {
            log.error("Failed to send price alert for {}", task.getStockCode(), e);
        }
    }


    /**
     * 更新 Redis 中的任务状态
     *
     * @param stockCode 股票代码
     * @param status    任务状态 (RUNNING/SUCCESS/FAILED)
     * @param result    执行结果描述
     * @param traceId   追踪ID
     */
    private void updateStatus(String stockCode, String status, String result, String traceId) {
        StockTaskStatus s = new StockTaskStatus();
        s.setStockCode(stockCode);
        s.setStatus(status);
        s.setOccupiedByNode(com.make.common.utils.ip.IpUtils.getHostIp());
        s.setOccupiedTime(System.currentTimeMillis());
        s.setTraceId(traceId);
        s.setLastResult(result);
        queueService.updateStatus(stockCode, s);
    }

    /**
     * 保存执行记录到数据库
     *
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @param status    最终状态
     * @param result    执行结果或错误信息
     * @param traceId   批次ID
     */
    private void saveExecutionRecord(String stockCode, String stockName, String status, String result, String traceId) {
        try {
            StockRefreshExecuteRecord record = new StockRefreshExecuteRecord();
            record.setStockCode(stockCode);
            record.setStockName(stockName);
            record.setStatus(status);
            record.setExecuteResult(result);
            record.setNodeIp(com.make.common.utils.ip.IpUtils.getHostIp());
            record.setExecuteTime(new Date());
            record.setTraceId(traceId);
            recordService.insertStockRefreshExecuteRecord(record);
        } catch (Exception e) {
            log.error("Failed to save execution record. stockCode={}, traceId={}", stockCode, traceId, e);
        }
    }

    /**
     * 线程休眠（忽略中断异常）
     *
     * @param ms 休眠毫秒数
     */
    private void sleepQuiet(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
