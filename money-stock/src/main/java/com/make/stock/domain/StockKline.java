package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import lombok.experimental.Accessors;

/**
 * 股票K线数据对象 stock_kline
 *
 * @author erqi
 * @date 2025-11-03
 */
@Accessors(chain = true)
public class StockKline extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 股票代码，例如 600519 */
    @Excel(name = "股票代码，例如 600519")
    private String stockCode;

    /** 市场标识，如 SH、SZ */
    @Excel(name = "市场标识，如 SH、SZ")
    private String market;

    /** 交易日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeDate;

    /** 开盘价 */
    @Excel(name = "开盘价")
    private BigDecimal open;

    /** 收盘价 */
    @Excel(name = "收盘价")
    private BigDecimal close;

    /** 最高价 */
    @Excel(name = "最高价")
    private BigDecimal high;

    /** 最低价 */
    @Excel(name = "最低价")
    private BigDecimal low;

    /** 成交量（股） */
    @Excel(name = "成交量", readConverterExp = "成交量（股）")
    private Long volume;

    /** 成交额（元） */
    @Excel(name = "成交额", readConverterExp = "成交额（元）")
    private BigDecimal amount;

    /** 涨跌额 */
    @Excel(name = "涨跌额")
    private BigDecimal change;

    /** 涨跌幅(%) */
    @Excel(name = "涨跌幅(%)")
    private BigDecimal changePercent;

    /** 换手率(%) */
    @Excel(name = "换手率(%)")
    private BigDecimal turnoverRatio;

    /** 前收盘价 */
    @Excel(name = "前收盘价")
    private BigDecimal preClose;

    /**
     * 查询开始日期（用于筛选最近 N 天）
     */
    private Date startDate;

    /**
     * 查询结束日期
     */
    private Date endDate;

    public StockKline setId(String id)
    {
        this.id = id;
        return this;
    }

    public String getId()
    {
        return id;
    }

    public StockKline setStockCode(String stockCode)
    {
        this.stockCode = stockCode;
        return this;
    }

    public String getStockCode()
    {
        return stockCode;
    }

    public StockKline setMarket(String market)
    {
        this.market = market;
        return this;
    }

    public String getMarket()
    {
        return market;
    }

    public StockKline setTradeDate(Date tradeDate)
    {
        this.tradeDate = tradeDate;
        return this;
    }

    public Date getTradeDate()
    {
        return tradeDate;
    }

    public StockKline setOpen(BigDecimal open)
    {
        this.open = open;
        return this;
    }

    public BigDecimal getOpen()
    {
        return open;
    }

    public StockKline setClose(BigDecimal close)
    {
        this.close = close;
        return this;
    }

    public BigDecimal getClose()
    {
        return close;
    }

    public StockKline setHigh(BigDecimal high)
    {
        this.high = high;
        return this;
    }

    public BigDecimal getHigh()
    {
        return high;
    }

    public StockKline setLow(BigDecimal low)
    {
        this.low = low;
        return this;
    }

    public BigDecimal getLow()
    {
        return low;
    }

    public StockKline setVolume(Long volume)
    {
        this.volume = volume;
        return this;
    }

    public Long getVolume()
    {
        return volume;
    }

    public StockKline setAmount(BigDecimal amount)
    {
        this.amount = amount;
        return this;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public StockKline setChange(BigDecimal change)
    {
        this.change = change;
        return this;
    }

    public BigDecimal getChange()
    {
        return change;
    }

    public StockKline setChangePercent(BigDecimal changePercent)
    {
        this.changePercent = changePercent;
        return this;
    }

    public BigDecimal getChangePercent()
    {
        return changePercent;
    }

    public StockKline setTurnoverRatio(BigDecimal turnoverRatio)
    {
        this.turnoverRatio = turnoverRatio;
        return this;
    }

    public BigDecimal getTurnoverRatio()
    {
        return turnoverRatio;
    }

    public StockKline setPreClose(BigDecimal preClose)
    {
        this.preClose = preClose;
        return this;
    }

    public BigDecimal getPreClose()
    {
        return preClose;
    }

    public Date getStartDate() {
        return startDate;
    }

    public StockKline setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date getEndDate() {
        return endDate;
    }

    public StockKline setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("stockCode", getStockCode())
            .append("market", getMarket())
            .append("tradeDate", getTradeDate())
            .append("open", getOpen())
            .append("close", getClose())
            .append("high", getHigh())
            .append("low", getLow())
            .append("volume", getVolume())
            .append("amount", getAmount())
            .append("change", getChange())
            .append("changePercent", getChangePercent())
            .append("turnoverRatio", getTurnoverRatio())
            .append("preClose", getPreClose())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
