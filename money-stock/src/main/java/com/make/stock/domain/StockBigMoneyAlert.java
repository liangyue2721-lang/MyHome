package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 大资金入场异动预警对象 stock_big_money_alert
 *
 * @author erqi
 * @date 2026-01-27
 */
public class StockBigMoneyAlert extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 股票代码，例如 600900
     */
    @Excel(name = "股票代码，例如 600900")
    private String stockCode;

    /**
     * 交易日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeDate;

    /**
     * 成交时刻
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "成交时刻", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeTime;

    /**
     * 成交价格
     */
    @Excel(name = "成交价格")
    private BigDecimal price;

    /**
     * 成交数量(股)
     */
    @Excel(name = "成交数量(股)")
    private Long volume;

    /**
     * 每笔平均成交量 (volume / tick_count)
     */
    @Excel(name = "每笔平均成交量 (volume / tick_count)")
    private BigDecimal avgVol;

    /**
     * 预警描述（例如：疑似大资金扫货）
     */
    @Excel(name = "预警描述", readConverterExp = "例=如：疑似大资金扫货")
    private String alertMsg;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
    }

    public Date getTradeTime() {
        return tradeTime;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getVolume() {
        return volume;
    }

    public void setAvgVol(BigDecimal avgVol) {
        this.avgVol = avgVol;
    }

    public BigDecimal getAvgVol() {
        return avgVol;
    }

    public void setAlertMsg(String alertMsg) {
        this.alertMsg = alertMsg;
    }

    public String getAlertMsg() {
        return alertMsg;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("stockCode", getStockCode())
                .append("tradeDate", getTradeDate())
                .append("tradeTime", getTradeTime())
                .append("price", getPrice())
                .append("volume", getVolume())
                .append("avgVol", getAvgVol())
                .append("alertMsg", getAlertMsg())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
