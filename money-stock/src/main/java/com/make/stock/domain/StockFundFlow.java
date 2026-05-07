package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 主力资金流向对象 stock_fund_flow
 *
 * @author erqi
 * @date 2026-05-07
 */
public class StockFundFlow extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 交易日期 (按天分区或查询过滤使用)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易日期 (按天分区或查询过滤使用)", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeDate;

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
     * 市场类型: 0-深交所, 1-上交所
     */
    @Excel(name = "市场类型: 0-深交所, 1-上交所")
    private Long marketType;

    /**
     * 最新价 (元)
     */
    @Excel(name = "最新价 (元)")
    private BigDecimal latestPrice;

    /**
     * 涨跌幅 (%)
     */
    @Excel(name = "涨跌幅 (%)")
    private BigDecimal changePercent;

    /**
     * 当天成交总额 (元)
     */
    @Excel(name = "当天成交总额 (元)")
    private BigDecimal totalAmount;

    /**
     * 主力净流入净额 (元)
     */
    @Excel(name = "主力净流入净额 (元)")
    private BigDecimal mainNetInflow;

    /**
     * 主力净流入占比 (%)
     */
    @Excel(name = "主力净流入占比 (%)")
    private BigDecimal mainInflowRatio;

    /**
     * 超大单净流入额 (元)
     */
    @Excel(name = "超大单净流入额 (元)")
    private BigDecimal superLargeInflow;

    /**
     * 超大单净流入占比 (%)
     */
    @Excel(name = "超大单净流入占比 (%)")
    private BigDecimal superLargeRatio;

    /**
     * 大单净流入额 (元)
     */
    @Excel(name = "大单净流入额 (元)")
    private BigDecimal largeInflow;

    /**
     * 大单净流入占比 (%)
     */
    @Excel(name = "大单净流入占比 (%)")
    private BigDecimal largeRatio;

    /**
     * 中单净流入额 (元)
     */
    @Excel(name = "中单净流入额 (元)")
    private BigDecimal mediumInflow;

    /**
     * 中单净流入占比 (%)
     */
    @Excel(name = "中单净流入占比 (%)")
    private BigDecimal mediumRatio;

    /**
     * 小单净流入额 (元)
     */
    @Excel(name = "小单净流入额 (元)")
    private BigDecimal smallInflow;

    /**
     * 小单净流入占比 (%)
     */
    @Excel(name = "小单净流入占比 (%)")
    private BigDecimal smallRatio;

    /**
     * 源数据更新时间戳 (Unix秒)
     */
    @Excel(name = "源数据更新时间戳 (Unix秒)")
    private Long dataTimestamp;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    public Date getTradeDate() {
        return tradeDate;
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

    public void setMarketType(Long marketType) {
        this.marketType = marketType;
    }

    public Long getMarketType() {
        return marketType;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setMainNetInflow(BigDecimal mainNetInflow) {
        this.mainNetInflow = mainNetInflow;
    }

    public BigDecimal getMainNetInflow() {
        return mainNetInflow;
    }

    public void setMainInflowRatio(BigDecimal mainInflowRatio) {
        this.mainInflowRatio = mainInflowRatio;
    }

    public BigDecimal getMainInflowRatio() {
        return mainInflowRatio;
    }

    public void setSuperLargeInflow(BigDecimal superLargeInflow) {
        this.superLargeInflow = superLargeInflow;
    }

    public BigDecimal getSuperLargeInflow() {
        return superLargeInflow;
    }

    public void setSuperLargeRatio(BigDecimal superLargeRatio) {
        this.superLargeRatio = superLargeRatio;
    }

    public BigDecimal getSuperLargeRatio() {
        return superLargeRatio;
    }

    public void setLargeInflow(BigDecimal largeInflow) {
        this.largeInflow = largeInflow;
    }

    public BigDecimal getLargeInflow() {
        return largeInflow;
    }

    public void setLargeRatio(BigDecimal largeRatio) {
        this.largeRatio = largeRatio;
    }

    public BigDecimal getLargeRatio() {
        return largeRatio;
    }

    public void setMediumInflow(BigDecimal mediumInflow) {
        this.mediumInflow = mediumInflow;
    }

    public BigDecimal getMediumInflow() {
        return mediumInflow;
    }

    public void setMediumRatio(BigDecimal mediumRatio) {
        this.mediumRatio = mediumRatio;
    }

    public BigDecimal getMediumRatio() {
        return mediumRatio;
    }

    public void setSmallInflow(BigDecimal smallInflow) {
        this.smallInflow = smallInflow;
    }

    public BigDecimal getSmallInflow() {
        return smallInflow;
    }

    public void setSmallRatio(BigDecimal smallRatio) {
        this.smallRatio = smallRatio;
    }

    public BigDecimal getSmallRatio() {
        return smallRatio;
    }

    public void setDataTimestamp(Long dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }

    public Long getDataTimestamp() {
        return dataTimestamp;
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
                .append("tradeDate", getTradeDate())
                .append("stockCode", getStockCode())
                .append("stockName", getStockName())
                .append("marketType", getMarketType())
                .append("latestPrice", getLatestPrice())
                .append("changePercent", getChangePercent())
                .append("totalAmount", getTotalAmount())
                .append("mainNetInflow", getMainNetInflow())
                .append("mainInflowRatio", getMainInflowRatio())
                .append("superLargeInflow", getSuperLargeInflow())
                .append("superLargeRatio", getSuperLargeRatio())
                .append("largeInflow", getLargeInflow())
                .append("largeRatio", getLargeRatio())
                .append("mediumInflow", getMediumInflow())
                .append("mediumRatio", getMediumRatio())
                .append("smallInflow", getSmallInflow())
                .append("smallRatio", getSmallRatio())
                .append("dataTimestamp", getDataTimestamp())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
