package com.make.quartz.service.impl;

import com.make.stock.domain.KlineData;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.StockKlineTask;
import com.make.stock.domain.dto.ProcessResult;
import com.make.stock.service.IStockKlineService;
import com.make.stock.util.KlineDataFetcher;
import com.make.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 单支股票 K 线任务处理器
 * ---------------------------------------------------------
 * 设计定位（非常关键）：
 * <p>
 * 1. **不直接落库**：
 * - 本类仅负责从 Python / API 拉取数据、解析、组装 Kline 实体
 * - 最终 insert/update 由调度端执行批处理，避免逐条 IO
 * <p>
 * 2. **任务类型区分**：
 * - taskStatus == 3 => 今日任务，需要判断是否存在 => insert / update 分发
 * - 其他任务 => 历史任务，无需 exists 判断 => 全部 insert
 * <p>
 * 3. **失败处理模型**：
 * - 每一条数据解析失败不影响后续
 * - ProcessResult.failedCount 记录解析失败条数，避免全量失败
 * <p>
 * 4. **最重要点**：
 * - 单任务执行期间完全无任何数据库 IO
 * - 只构造 ProcessResult，调度端汇总结果一次 commit
 * <p>
 * ---------------------------------------------------------
 * => 非常适合大规模多股票批处理与高吞吐量 Quartz 执行
 * ---------------------------------------------------------
 */
@Component
public class StockKlineProcessor {

    private static final Logger log = LoggerFactory.getLogger(StockKlineProcessor.class);

    @Resource
    private StockKlineBuilder klineBuilder;

    @Resource
    private StockKlineRetryFetcher retryFetcher;

    @Resource
    private StockKlineRepositoryService repositoryService;


    /**
     * 核心执行入口
     *
     * @param task 股票任务实体
     * @param df   日期格式器（如 yyyy-MM-dd）
     * @return ProcessResult（包含 insertList、updateList、失败统计、成功标识）
     * <p>
     * 设计亮点：
     * - ProcessResult 与库操作解耦
     * - 允许调度层在多线程环境下汇总所有 ProcessResult，再统一事务提交
     */
    public ProcessResult processTaskData(StockKlineTask task, DateTimeFormatter df) {

        ProcessResult result = new ProcessResult();
        result.stockCode = task.getStockCode();

        String stockCode = task.getStockCode();

        log.info("▶ 开始执行单任务 stock={} taskStatus={}", stockCode, task.getTaskStatus());

        try {
            if (Long.valueOf(3).equals(task.getTaskStatus())) {
                // === 今日任务 ===
                handleToday(task, df, result);
            } else {
                // === 历史任务 ===
                handleHistory(task, df, result);
            }

            result.success = true;

            log.info("✓ 单任务完成 stock={} insert={} update={} failed={}",
                    stockCode,
                    result.insertList.size(),
                    result.updateList.size(),
                    result.failedCount);

        } catch (Exception e) {
            result.success = false;
            log.error("❌ 任务执行异常 stock={} : {}", stockCode, e.getMessage(), e);
        }

        return result;
    }


    // =======================================================
    // 今日任务处理
    // =======================================================

    /**
     * 今日任务处理
     * <p>
     * 核心逻辑：
     * ------------------------------------------------------
     * 1. 调 KlineDataFetcher.fetchKlineDataFiveDay 拉取最近 N 天数据
     * （原因：今日可能盘中无成交，需要保证有数据）
     * <p>
     * 2. 遍历 KlineData，解析 tradeDate + 构造 StockKline
     * <p>
     * 3. 一次性查询数据库是否存在 tradeDate（批量 exists）
     * repositoryService.selectExistsDates(stockCode, tradeDateList)
     * <p>
     * 4. 批量分发 insertList / updateList，避免循环 exists
     * <p>
     * ------------------------------------------------------
     * 优势：
     * - 大幅降低 DB IO
     * - insert / update 的权衡完全按 tradeDate 批处理
     */
    private void handleToday(StockKlineTask task,
                             DateTimeFormatter df,
                             ProcessResult result) {

        String stockCode = task.getStockCode();
        String market = StockMarketResolver.getMarketCode(stockCode);

        // [VALIDATION] 校验 Market 是否合法，若不合法直接终止
        if (market == null) {
            log.warn("⚠ [INVALID_MARKET] 无法解析市场编码，终止任务 stock={}", stockCode);
            result.failedCount++;
            return;
        }

        log.info("▶ 今日任务拉取分钟级数据 stock={} market={}", stockCode, market);

        // ====== 拉取 Python 分钟级数据并聚合成日级（内部 fresh session） ======
        List<KlineData> todayData = KlineDataFetcher.fetchKlineDataFiveDay(stockCode, market);

        if (todayData == null || todayData.isEmpty()) {
            // 不视为异常，仅记录
            result.failedCount++;
            log.warn("⚠ 今日任务无数据 stock={}", stockCode);
            return;
        }

        log.info("✓ 今日数据获取成功 stock={} rows={}", stockCode, todayData.size());

        // ====== 缓存所有构造后的 StockKline 和 tradeDate ======
        List<StockKline> allEntities = new ArrayList<>();
        List<LocalDate> tradeDateList = new ArrayList<>();

        // ====== 循环构造实体，忽略解析失败 ======
        for (KlineData d : todayData) {

            if (d == null || d.getTradeDate() == null) {
                result.failedCount++;
                continue;
            }

            // tradeDate 解析
            LocalDate tradeDate = klineBuilder.parseTradeDate(d.getTradeDate(), df);
            if (tradeDate == null) {
                result.failedCount++;
                continue;
            }

            // 组装 StockKline
            StockKline entity = klineBuilder.build(stockCode, market, tradeDate, d);
            allEntities.add(entity);
            tradeDateList.add(tradeDate);
        }

        if (allEntities.isEmpty()) {
            log.warn("⚠ 构造实体为空 stock={}", stockCode);
            return;
        }

        log.info("▶ 今日任务批量 exists 查询 stock={} dates={}", stockCode, tradeDateList.size());

        // ====== 一次性 exists 查询 ======
        List<LocalDate> existsDates =
                repositoryService.selectExistsDates(stockCode, tradeDateList);

        log.info("✓ exists 查询完成 stock={} exists={}", stockCode, existsDates.size());

        // ====== insert / update 分发 ======
        for (StockKline k : allEntities) {
            if (existsDates.contains(k.getTradeDate())) {
                result.updateList.add(k);
            } else {
                result.insertList.add(k);
            }
        }

        log.info("✓ 今日任务结果 stock={} insert={} update={} failed={}",
                stockCode,
                result.insertList.size(),
                result.updateList.size(),
                result.failedCount);
    }


    // =======================================================
    // 历史任务处理
    // =======================================================

    /**
     * 历史任务处理
     * <p>
     * 核心逻辑：
     * ------------------------------------------------------
     * 1. Python + retryFetcher 拉取全历史日级数据
     * （可能几十年）
     * <p>
     * 2. 不需要 exists 查询
     * => result.insertList 全部落库
     * <p>
     * 3. 单条解析错误计入 failedCount，不影响整体
     * <p>
     * ------------------------------------------------------
     * 最重要设计原则：
     * - **不要 exists**：批量 insert 最快
     */
    private void handleHistory(StockKlineTask task,
                               DateTimeFormatter df,
                               ProcessResult result) {

        String stockCode = task.getStockCode();
        String market = StockMarketResolver.getMarketCode(stockCode);

        // [VALIDATION] 校验 Market 是否合法，若不合法直接终止
        if (market == null) {
            log.warn("⚠ [INVALID_MARKET] 无法解析市场编码，终止任务 stock={}", stockCode);
            result.failedCount++;
            return;
        }

        log.info("▶ 历史任务全历史拉取 stock={} market={}", stockCode, market);

        // Python 重试逻辑（内部支持 fresh session + retry）
        List<KlineData> klineList = retryFetcher.fetchWithRetry(stockCode, market);

        if (klineList == null || klineList.isEmpty()) {
            result.success = false;
            log.warn("⚠ 历史任务无数据 stock={}", stockCode);
            return;
        }

        log.info("✓ 历史数据获取成功 stock={} rows={}", stockCode, klineList.size());

        // 全部作为 insertList，不 exists
        for (KlineData data : klineList) {

            if (data == null || data.getTradeDate() == null) {
                result.failedCount++;
                continue;
            }

            LocalDate tradeDate = klineBuilder.parseTradeDate(data.getTradeDate(), df);
            if (tradeDate == null) {
                result.failedCount++;
                continue;
            }

            StockKline entity = klineBuilder.build(stockCode, market, tradeDate, data);
            result.insertList.add(entity);
        }

        log.info("✓ 历史任务 stock={} insert={} failed={}",
                stockCode,
                result.insertList.size(),
                result.failedCount);
    }

}
