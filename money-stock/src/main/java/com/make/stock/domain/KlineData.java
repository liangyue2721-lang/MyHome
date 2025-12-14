package com.make.stock.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.Date;

/**
 * K线数据实体类
 * <p>
 * 用于表示股票的K线数据，包括开盘价、收盘价、最高价、最低价等信息
 * </p>
 */
public class KlineData {
    /**
     * 交易日期
     */
    @JSONField(name = "trade_date")
    private String tradeDate;

    /**
     * 交易时间
     */
    @JSONField(name = "trade_time")
    private Date tradeTime;

    /**
     * 股票代码
     */
    @JSONField(name = "stock_code")
    private String stockCode;

    /**
     * 开盘价
     */
    @JSONField(name = "open")
    private Double open;

    /**
     * 收盘价
     */
    @JSONField(name = "close")
    private Double close;

    /**
     * 最高价
     */
    @JSONField(name = "high")
    private Double high;

    /**
     * 最低价
     */
    @JSONField(name = "low")
    private Double low;

    /**
     * 成交量
     */
    @JSONField(name = "volume")
    private Long volume;

    /**
     * 成交额
     */
    @JSONField(name = "amount")
    private Double amount;

    /**
     * 涨跌额
     */
    @JSONField(name = "change")
    private Double change;

    /**
     * 涨跌幅(%)
     */
    @JSONField(name = "change_percent")
    private Double changePercent;

    /**
     * 换手率(%)
     */
    @JSONField(name = "turnover_ratio")
    private Double turnoverRatio;

    /**
     * 前收盘价
     */
    @JSONField(name = "pre_close")
    private Double preClose;

    // -------------------- Setter 方法（链式） --------------------

    public KlineData setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
        return this;
    }

    public KlineData setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
        return this;
    }

    public KlineData setStockCode(String stockCode) {
        this.stockCode = stockCode;
        return this;
    }

    public KlineData setOpen(Double open) {
        this.open = open;
        return this;
    }

    public KlineData setClose(Double close) {
        this.close = close;
        return this;
    }

    public KlineData setHigh(Double high) {
        this.high = high;
        return this;
    }

    public KlineData setLow(Double low) {
        this.low = low;
        return this;
    }

    public KlineData setVolume(Long volume) {
        this.volume = volume;
        return this;
    }

    public KlineData setAmount(Double amount) {
        this.amount = amount;
        return this;
    }

    public KlineData setChange(Double change) {
        this.change = change;
        return this;
    }

    public KlineData setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
        return this;
    }

    public KlineData setTurnoverRatio(Double turnoverRatio) {
        this.turnoverRatio = turnoverRatio;
        return this;
    }

    public KlineData setPreClose(Double preClose) {
        this.preClose = preClose;
        return this;
    }

    // -------------------- Getter 方法 --------------------

    public String getTradeDate() {
        return tradeDate;
    }

    public Date getTradeTime() {
        return tradeTime;
    }

    public String getStockCode() {
        return stockCode;
    }

    public Double getOpen() {
        return open;
    }

    public Double getClose() {
        return close;
    }

    public Double getHigh() {
        return high;
    }

    public Double getLow() {
        return low;
    }

    public Long getVolume() {
        return volume;
    }

    public Double getAmount() {
        return amount;
    }

    public Double getChange() {
        return change;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public Double getTurnoverRatio() {
        return turnoverRatio;
    }

    public Double getPreClose() {
        return preClose;
    }

    @Override
    public String toString() {
        return "KlineData{" +
                "tradeDate='" + tradeDate + '\'' +
                ", tradeTime=" + tradeTime +
                ", stockCode='" + stockCode + '\'' +
                ", open=" + open +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                ", amount=" + amount +
                ", change=" + change +
                ", changePercent=" + changePercent +
                ", turnoverRatio=" + turnoverRatio +
                ", preClose=" + preClose +
                '}';
    }
}