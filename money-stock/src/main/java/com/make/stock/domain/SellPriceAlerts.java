package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 卖出价位提醒对象 sell_price_alerts
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public class SellPriceAlerts extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 提醒日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提醒日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date alertDate;

    /**
     * 股票代码
     */
    @Excel(name = "股票代码")
    private String stockCode;

    /**
     * 股票名称
     */
    @Excel(name = "股票名称")
    private String stockName;

    /**
     * 最新价格
     */
    @Excel(name = "最新价格")
    private BigDecimal latestPrice;

    /**
     * 阈值价格
     */
    @Excel(name = "阈值价格")
    private BigDecimal thresholdPrice;

    /**
     * 通知次数
     */
    @Excel(name = "通知次数")
    private Long indexCount;

    /**
     * 是否在持仓，0：在持仓，1：未持仓
     */
    @Excel(name = "是否在持仓，0：在持仓，1：未持仓")
    private Integer isEnabled;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    /**
     * API 接口
     */
    @Excel(name = "API 接口")
    private String stockApi;


    public SellPriceAlerts setId(Long id) {
        this.id = id;
        return this;
    }

    public SellPriceAlerts setAlertDate(Date alertDate) {
        this.alertDate = alertDate;
        return this;
    }

    public SellPriceAlerts setStockCode(String stockCode) {
        this.stockCode = stockCode;
        return this;
    }

    public SellPriceAlerts setStockName(String stockName) {
        this.stockName = stockName;
        return this;
    }

    public SellPriceAlerts setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
        return this;
    }

    public SellPriceAlerts setThresholdPrice(BigDecimal thresholdPrice) {
        this.thresholdPrice = thresholdPrice;
        return this;
    }

    public SellPriceAlerts setIndexCount(Long indexCount) {
        this.indexCount = indexCount;
        return this;
    }

    public SellPriceAlerts setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
        return this;
    }

    public SellPriceAlerts setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public SellPriceAlerts setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public SellPriceAlerts setStockApi(String stockApi) {
        this.stockApi = stockApi;
        return this;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Date getAlertDate() {
        return alertDate;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }

    public Long getIndexCount() {
        return indexCount;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getStockApi() {
        return stockApi;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("alertDate", getAlertDate())
                .append("stockCode", getStockCode())
                .append("stockName", getStockName())
                .append("latestPrice", getLatestPrice())
                .append("thresholdPrice", getThresholdPrice())
                .append("indexCount", getIndexCount())
                .append("isEnabled", getIsEnabled())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("stockApi", getStockApi())
                .toString();
    }
}
