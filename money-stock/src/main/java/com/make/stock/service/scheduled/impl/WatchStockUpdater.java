package com.make.stock.service.scheduled.impl;

import com.make.common.utils.DateUtils;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.Watchstock;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.IStockKlineService;
import com.make.stock.service.IWatchstockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 自选股更新器
 * <p>
 * 负责从最新的K线数据中提取相关信息，更新自选股的各项指标数据。
 * 只对当日的K线数据生效，确保自选股信息的实时性。
 * </p>
 */
@Component
public class WatchStockUpdater {

    private static final Logger log = LoggerFactory.getLogger(WatchStockUpdater.class);

    /**
     * 自选股服务接口，用于获取和更新自选股信息
     */
    @Resource
    private IWatchstockService watchstockService;

    /**
     * 股票K线服务接口，用于执行具体的数据库操作
     */
    @Resource
    private IStockKlineService stockKlineService;

    /**
     * 处理自选股更新
     * <p>
     * 根据传入的K线数据更新对应的自选股信息，包括最新价格、涨跌额、涨跌幅等。
     * 只处理当日的K线数据，并更新周度高低价等统计信息。
     * </p>
     *
     * @param stockKline K线数据对象
     */
    public void processWatchStock(StockKline stockKline) {
        // 参数校验：如果K线数据为空，则直接返回
        if (stockKline == null) return;

        Date tradeDate = stockKline.getTradeDate();
        if (tradeDate == null) {
            log.warn("自选股 processWatchStock {} Invalid trade date: {}", stockKline.getStockCode(), tradeDate);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 判断是否为当天
        if (!sdf.format(tradeDate).equals(sdf.format(new Date()))) {
            log.warn("自选股 rocessWatchStock {} Invalid trade date: {}", stockKline.getStockCode(), tradeDate);
            return;
        }


        // 根据股票代码获取对应的自选股信息
        Watchstock watchStock = watchstockService.getWatchStockByCode(stockKline.getStockCode());
        if (watchStock == null) {
            // 如果未找到对应自选股，则直接返回
            log.warn("自选股 processWatchStock {} 未找到对应自选股", stockKline.getStockCode());
            return;
        }
        log.info("自选股 processWatchStock {} 获取自选股信息成功,ID {}", stockKline.getStockCode(), watchStock.getId());

        // 更新自选股的价格相关信息
        updateWatchStockPrices(watchStock, stockKline);
        // 根据需要更新周度高低价
        updateWeekHighLowIfNeeded(watchStock);

        // 更新最后修改时间
        watchStock.setUpdateTime(DateUtils.getNowDate());
        // 保存更新后的自选股信息
        watchstockService.updateWatchstock(watchStock);

        // 记录更新成功的日志信息
        log.info("✅ 自选股 [{}] 更新：现价 {}，日高 {}，日低 {}，周低 {}，年低 {}",
                watchStock.getCode(),
                watchStock.getNewPrice(),
                watchStock.getHighPrice(),
                watchStock.getLowPrice(),
                watchStock.getWeekLow(),
                watchStock.getYearLow());
    }

    /**
     * 从实时数据更新自选股 (Direct from StockRealtimeInfo)
     *
     * @param watchstock 自选股对象
     * @param info       实时行情数据
     */
    public void updateFromRealtimeInfo(Watchstock watchstock, StockRealtimeInfo info) {
        if (watchstock == null || info == null) {
            log.warn("自选股或实时行情数据为空，无法更新股票信息");
            return;
        }

        if (info.getOpenPrice() == null) {
            log.warn("实时行情价格数据为空，股票代码: {}", watchstock.getCode());
            return;
        }

        watchstock.setNewPrice(BigDecimal.valueOf(info.getPrice()));
        watchstock.setHighPrice(BigDecimal.valueOf(info.getHighPrice()));
        watchstock.setLowPrice(BigDecimal.valueOf(info.getLowPrice()));
//        watchstock.setPreviousClose(BigDecimal.valueOf(info.getPrevClose()));

        // 计算涨跌
        BigDecimal close = watchstock.getNewPrice();
        BigDecimal preClose = watchstock.getPreviousClose();

        if (preClose != null && preClose.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal upsDowns = close.subtract(preClose).setScale(2, RoundingMode.HALF_UP);
            BigDecimal upsDownsRate = upsDowns.divide(preClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            watchstock.setUpsDowns(upsDowns);
            watchstock.setChangeRate(upsDownsRate);
        }

        // 周高低逻辑
        updateWeekHighLowIfNeeded(watchstock);
        // 今年高低逻辑
        updateYearHighLowIfNeeded(watchstock);

        // 更新时间
        watchstock.setUpdateTime(DateUtils.getNowDate());
        watchstock.setThresholdPrice(null);
        watchstock.setNum(null);
    }

    /**
     * 更新自选股价格相关信息
     * <p>
     * 根据K线数据更新自选股的最新价格、涨跌额、涨跌幅、最高价、最低价等信息。
     * </p>
     *
     * @param watchStock 自选股对象
     * @param stockKline K线数据对象
     */
    private void updateWatchStockPrices(Watchstock watchStock, StockKline stockKline) {
        // 参数校验：如果任一参数为空，则直接返回
        if (watchStock == null || stockKline == null) return;

        // 获取收盘价和昨收价
        BigDecimal close = stockKline.getClose();
        BigDecimal preClose = stockKline.getPreClose();

        // 数据完整性校验：如果缺少关键价格数据，则记录警告日志并返回
        if (close == null || preClose == null) {
            log.warn("缺少收盘/昨收数据，跳过自选股 {} 更新", watchStock.getCode());
            return;
        }

        // 计算涨跌额（收盘价 - 昨收价），保留2位小数
        BigDecimal upsDowns = close.subtract(preClose).setScale(2, RoundingMode.HALF_UP);

        // 计算涨跌幅：(涨跌额 / 昨收价) * 100%，保留2位小数
        BigDecimal upsDownsRate = preClose.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : upsDowns.divide(preClose, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // 更新自选股的各项价格指标
        watchStock.setNewPrice(close)                     // 设置最新价格
                .setUpsDowns(upsDowns)                    // 设置涨跌额
                .setChangeRate(upsDownsRate)              // 设置涨跌幅
                .setPreviousClose(preClose)               // 设置昨收价
                .setLowPrice(stockKline.getLow())         // 设置当日最低价
                .setHighPrice(stockKline.getHigh())       // 设置当日最高价
                .setNum(null)                             // 清空Num字段
                .setThresholdPrice(null);                 // 清空阈值价格字段
    }

    /**
     * 根据本周（周一至周五）行情按需更新周度最高价与最低价。
     *
     * <p>规则：
     * <ul>
     *   <li>若本周 K 线可用，则以本周计算结果为准（可覆盖修正旧值）</li>
     *   <li>若本周 K 线不可用，则仅使用当日实时行情做初始化/突破更新</li>
     *   <li>当日实时行情用于兜底补偿，防止当日 K 线未入库</li>
     * </ul>
     *
     * @param stock 自选股对象（需包含当日实时高低价）
     */
    public void updateWeekHighLowIfNeeded(Watchstock stock) {

        // 股票代码
        String stockCode = stock.getCode();

        // 取当前日期（周末视为上一交易日，避免把周末算作新的一周）
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            today = today.minusDays(1);
        } else if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            today = today.minusDays(2);
        }

        // 本周周一（ISO：周一为一周开始）
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 构造本周周一至周五日期列表
        List<LocalDate> tradeDateList = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            tradeDateList.add(monday.plusDays(i));
        }

        // 查询本周 K 线（允许为空）
        List<StockKline> stockKlineList =
                stockKlineService.queryWeekAllStockKline(stockCode, tradeDateList);

        BigDecimal computedWeekHigh = null;
        BigDecimal computedWeekLow = null;

        // 基于 K 线计算本周高低价
        if (stockKlineList != null && !stockKlineList.isEmpty()) {
            for (StockKline kline : stockKlineList) {
                if (kline.getHigh() != null) {
                    computedWeekHigh = (computedWeekHigh == null)
                            ? kline.getHigh()
                            : computedWeekHigh.max(kline.getHigh());
                }
                if (kline.getLow() != null) {
                    computedWeekLow = (computedWeekLow == null)
                            ? kline.getLow()
                            : computedWeekLow.min(kline.getLow());
                }
            }
        }

        // 当日实时行情兜底补偿（防止当日 K 线尚未入库）
        if (stock.getHighPrice() != null) {
            computedWeekHigh = (computedWeekHigh == null)
                    ? stock.getHighPrice()
                    : computedWeekHigh.max(stock.getHighPrice());
        }
        if (stock.getLowPrice() != null) {
            computedWeekLow = (computedWeekLow == null)
                    ? stock.getLowPrice()
                    : computedWeekLow.min(stock.getLowPrice());
        }

        // 若本周既无 K 线也无今日行情，则无从计算
        if (computedWeekHigh == null && computedWeekLow == null) {
            log.warn("股票 [{}] 本周无有效行情数据，跳过周高/周低更新", stockCode);
            return;
        }

        // 本周有 K 线：以本周计算结果为准，允许覆盖修正（解决跨周残留问题）
        if (stockKlineList != null && !stockKlineList.isEmpty()) {

            boolean changed = false;

            if (computedWeekLow != null && (stock.getWeekLow() == null || computedWeekLow.compareTo(stock.getWeekLow()) != 0)) {
                stock.setWeekLow(computedWeekLow);
                changed = true;
            }

            if (computedWeekHigh != null && (stock.getWeekHigh() == null || computedWeekHigh.compareTo(stock.getWeekHigh()) != 0)) {
                stock.setWeekHigh(computedWeekHigh);
                changed = true;
            }

            if (changed) {
                log.info("股票 [{}] 周度高低价按本周K线校准，高={}, 低={}", stockCode, computedWeekHigh, computedWeekLow);
            }
            return;
        }

        // 本周无 K 线：仅用当日行情做初始化/突破更新（保守，不做重置覆盖）
        if (computedWeekLow != null &&
                (stock.getWeekLow() == null || computedWeekLow.compareTo(stock.getWeekLow()) < 0)) {
            stock.setWeekLow(computedWeekLow);
            log.info("股票 [{}] 周最低价更新为 {}", stockCode, computedWeekLow);
        }

        if (computedWeekHigh != null &&
                (stock.getWeekHigh() == null || computedWeekHigh.compareTo(stock.getWeekHigh()) > 0)) {
            stock.setWeekHigh(computedWeekHigh);
            log.info("股票 [{}] 周最高价更新为 {}", stockCode, computedWeekHigh);
        }
    }


    /**
     * 根据年初至今的行情数据，按需更新股票的年度最高价和最低价。
     *
     * <p>处理逻辑：
     * <ol>
     *   <li>以当年 1 月 1 日至今日为时间范围，查询历史 K 线数据</li>
     *   <li>遍历计算区间内的最高价和最低价</li>
     *   <li>将计算结果与今日实时行情进行兜底合并</li>
     *   <li>仅在突破当前已记录的年高/年低时才执行更新</li>
     * </ol>
     *
     * <p>设计目的：
     * <ul>
     *   <li>避免因 K 线未及时入库导致年高/年低遗漏</li>
     *   <li>避免无意义的覆盖更新</li>
     *   <li>保证年高/年低数据的单调性与正确性</li>
     * </ul>
     *
     * @param stock 股票基础信息（需包含今日实时高低价）
     */
    public void updateYearHighLowIfNeeded(Watchstock stock) {
        String stockCode = stock.getCode();
        LocalDate today = LocalDate.now();

        // 本年起始日期
        LocalDate startOfYear = LocalDate.of(today.getYear(), 1, 1);

        // 构造 K 线查询条件
        StockKline query = new StockKline();
        query.setStockCode(stockCode);
        query.setStartDate(Date.from(startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // 查询年内 K 线
        List<StockKline> klineList = stockKlineService.selectStockKlineList(query);

        BigDecimal computedYearHigh = null;
        BigDecimal computedYearLow = null;

        // 计算年内历史最高/最低
        if (klineList != null && !klineList.isEmpty()) {
            for (StockKline k : klineList) {
                if (k.getHigh() != null) {
                    computedYearHigh = (computedYearHigh == null)
                            ? k.getHigh()
                            : computedYearHigh.max(k.getHigh());
                }
                if (k.getLow() != null) {
                    computedYearLow = (computedYearLow == null)
                            ? k.getLow()
                            : computedYearLow.min(k.getLow());
                }
            }
        }

        // 今日实时行情兜底（防止今日 K 线未入库）
        if (stock.getHighPrice() != null) {
            computedYearHigh = (computedYearHigh == null)
                    ? stock.getHighPrice()
                    : computedYearHigh.max(stock.getHighPrice());
        }
        if (stock.getLowPrice() != null) {
            computedYearLow = (computedYearLow == null)
                    ? stock.getLowPrice()
                    : computedYearLow.min(stock.getLowPrice());
        }

        // 年最低价更新（仅当更低时）
        if (computedYearLow != null &&
                (stock.getYearLow() == null || computedYearLow.compareTo(stock.getYearLow()) < 0)) {
            stock.setYearLow(computedYearLow);
            log.info("股票 [{}] 年最低价更新为 {}", stockCode, computedYearLow);
        }

        // 年最高价更新（仅当更高时）
        if (computedYearHigh != null &&
                (stock.getYearHigh() == null || computedYearHigh.compareTo(stock.getYearHigh()) > 0)) {
            stock.setYearHigh(computedYearHigh);
            log.info("股票 [{}] 年最高价更新为 {}", stockCode, computedYearHigh);
        }
    }


    public void batchUpdateWatchStock(List<Watchstock> watchstocks) {
        watchstockService.updateWatchstockBatch(watchstocks);
    }
}
