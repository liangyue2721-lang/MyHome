package com.make.quartz.service.impl;

import com.make.common.utils.DateUtils;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
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
     * 根据需要更新周度高低价
     * <p>
     * 如果当前的周度高低价为null，或者当日高低价突破了周度记录，则更新周度高低价。
     * </p>
     *
     * @param stock 自选股对象
     */
    public void updateWeekHighLowIfNeeded(Watchstock stock) {
        // 标记是否发生了更新操作
        boolean updated = false;
        // 获取当日最高价和最低价
        BigDecimal highPrice = stock.getHighPrice();
        BigDecimal lowPrice = stock.getLowPrice();

        // 检查并更新周度最低价
        if (stock.getWeekLow() == null || (lowPrice != null && lowPrice.compareTo(stock.getWeekLow()) < 0)) {
            // 如果周度最低价为null或者当日最低价低于周度最低价，则更新
            stock.setWeekLow(lowPrice);
            updated = true;
            log.info("股票 [{}] 周度低价已更新为 {}", stock.getCode(), lowPrice);
        }

        // 检查并更新周度最高价
        if (stock.getWeekHigh() == null || (highPrice != null && highPrice.compareTo(stock.getWeekHigh()) > 0)) {
            // 如果周度最高价为null或者当日最高价高于周度最高价，则更新
            stock.setWeekHigh(highPrice);
            updated = true;
            log.info("股票 [{}] 周度高价已更新为 {}", stock.getCode(), highPrice);
        }

        // 注意：WatchStock 的持久化在外层调用完成后统一执行
    }

    public void batchUpdateWatchStock(List<Watchstock> watchstocks) {
        watchstockService.updateWatchstockBatch(watchstocks);
    }
}
