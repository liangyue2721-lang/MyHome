package com.make.quartz.service.impl;

import com.make.common.utils.DateUtils;
import com.make.stock.domain.KlineData;
import com.make.stock.domain.StockKline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 股票K线数据构建器
 * <p>
 * 负责将原始的KlineData数据转换为StockKline实体对象，同时提供日期解析和数值安全转换功能。
 * 是连接原始数据和持久化实体之间的桥梁组件。
 * </p>
 */
@Component
public class StockKlineBuilder {

    private static final Logger log = LoggerFactory.getLogger(StockKlineBuilder.class);

    /**
     * 解析交易日期字符串
     * <p>
     * 将字符串格式的日期解析为LocalDate对象，如果解析耗时超过5毫秒会输出调试日志。
     * </p>
     *
     * @param tradeDateStr 交易日期字符串
     * @param df           日期格式化器
     * @return 解析后的LocalDate对象，解析失败返回null
     */
    public LocalDate parseTradeDate(String tradeDateStr, DateTimeFormatter df) {
        // 如果日期字符串为空，直接返回null
        if (tradeDateStr == null) return null;
        
        // 记录解析开始时间，用于性能监控
        long s = System.currentTimeMillis();
        try {
            // 执行日期解析操作
            LocalDate date = LocalDate.parse(tradeDateStr.trim(), df);
            // 计算解析耗时
            long t = System.currentTimeMillis() - s;
            
            // 如果解析耗时超过5毫秒，输出调试日志
            if (t > 5) {
                log.debug("⏱️ 日期解析耗时 {} ms：{}", t, tradeDateStr);
            }
            return date;
        } catch (Exception e) {
            // 解析失败时记录警告日志
            log.warn("❌ tradeDate 格式错误：{}，跳过。", tradeDateStr);
            return null;
        }
    }

    /**
     * 构建StockKline实体对象
     * <p>
     * 将原始KlineData数据转换为StockKline实体对象，并填充各种属性值。
     * 同时设置创建时间和更新时间。
     * </p>
     *
     * @param stockCode  股票代码
     * @param market     市场代码
     * @param tradeDate  交易日期
     * @param data       原始K线数据
     * @return 构建完成的StockKline实体对象
     */
    public StockKline build(String stockCode, String market, LocalDate tradeDate, KlineData data) {
        // 创建StockKline对象并设置基本属性
        StockKline stockKline = new StockKline()
                .setStockCode(stockCode)                              // 设置股票代码
                .setMarket(market)                                    // 设置市场代码
                .setTradeDate(java.sql.Date.valueOf(tradeDate))       // 设置交易日期
                .setOpen(safeBigDecimal(data.getOpen()))              // 设置开盘价
                .setClose(safeBigDecimal(data.getClose()))            // 设置收盘价
                .setHigh(safeBigDecimal(data.getHigh()))              // 设置最高价
                .setLow(safeBigDecimal(data.getLow()))                // 设置最低价
                .setChange(safeBigDecimal(data.getChange()))          // 设置涨跌额
                .setChangePercent(safeBigDecimal(data.getChangePercent()))  // 设置涨跌幅
                .setTurnoverRatio(safeBigDecimal(data.getTurnoverRatio()))  // 设置换手率
                .setVolume(data.getVolume())                          // 设置成交量
                .setPreClose(safeBigDecimal(data.getPreClose()))      // 设置昨收价
                .setAmount(safeBigDecimal(data.getAmount()));         // 设置成交金额

        // 设置创建时间和更新时间
        stockKline.setCreateTime(DateUtils.getNowDate());
        stockKline.setUpdateTime(DateUtils.getNowDate());
        return stockKline;
    }

    /**
     * 安全地将Double值转换为BigDecimal
     * <p>
     * 处理可能为null的Double值，避免空指针异常。
     * </p>
     *
     * @param val Double值
     * @return 转换后的BigDecimal值，如果原值为null则返回BigDecimal.ZERO
     */
    private BigDecimal safeBigDecimal(Double val) {
        // 如果值为null，返回零值；否则转换为BigDecimal
        return val == null ? BigDecimal.ZERO : BigDecimal.valueOf(val);
    }
}