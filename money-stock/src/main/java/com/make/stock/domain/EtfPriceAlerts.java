package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * ETF买入卖出价格提醒对象 etf_price_alerts
 *
 * @author erqi
 * @date 2025-06-24
 */
public class EtfPriceAlerts extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 提醒类型：buy=买入提醒，sell=卖出提醒
     */
    @Excel(name = "提醒类型：buy=买入提醒，sell=卖出提醒")
    private String alertType;

    /**
     * 提醒触发时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提醒触发时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date alertDate;

    /**
     * ETF代码
     */
    @Excel(name = "ETF代码")
    private String stockCode;

    /**
     * ETF名称
     */
    @Excel(name = "ETF名称")
    private String stockName;

    /**
     * 最新价格
     */
    @Excel(name = "最新价格")
    private BigDecimal latestPrice;

    /**
     * 触发提醒的价格阈值
     */
    @Excel(name = "触发提醒的价格阈值")
    private BigDecimal thresholdPrice;

    /**
     * 触发提醒的次数
     */
    @Excel(name = "触发提醒的次数")
    private Long indexCount;

    /**
     * 是否已持仓：0=持仓中，1=未持仓
     */
    @Excel(name = "是否已持仓：0=持仓中，1=未持仓")
    private Integer isEnabled;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertDate(Date alertDate) {
        this.alertDate = alertDate;
    }

    public Date getAlertDate() {
        return alertDate;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getStockName() {
        return stockName;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setThresholdPrice(BigDecimal thresholdPrice) {
        this.thresholdPrice = thresholdPrice;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }

    public void setIndexCount(Long indexCount) {
        this.indexCount = indexCount;
    }

    public Long getIndexCount() {
        return indexCount;
    }

    public void setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("alertType", getAlertType())
                .append("alertDate", getAlertDate())
                .append("stockCode", getStockCode())
                .append("stockName", getStockName())
                .append("latestPrice", getLatestPrice())
                .append("thresholdPrice", getThresholdPrice())
                .append("indexCount", getIndexCount())
                .append("isEnabled", getIsEnabled())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
