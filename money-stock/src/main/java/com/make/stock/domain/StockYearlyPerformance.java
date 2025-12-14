package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 股票当年现数据对象 stock_yearly_performance
 *
 * @author erqi
 * @date 2025-10-19
 */
public class StockYearlyPerformance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date date;

    /**
     * 股票代码
     */
    @Excel(name = "股票代码")
    private String code;

    /**
     * 股票名称
     */
    @Excel(name = "股票名称")
    private String name;

    /**
     * 最新价格
     */
    @Excel(name = "最新价格")
    private BigDecimal newPrice;

    /**
     * 昨收价格
     */
    @Excel(name = "昨收价格")
    private BigDecimal previousClose;

    /**
     * 当年最低价
     */
    @Excel(name = "当年最低价")
    private BigDecimal yearLowPrice;

    /**
     * 当年涨跌幅
     */
    @Excel(name = "当年涨跌幅")
    private BigDecimal yearLowPriceRate;

    /**
     * 阈值价格
     */
    @Excel(name = "阈值价格")
    private BigDecimal thresholdPrice;
    
    /**
     * 周内最高价
     */
    @Excel(name = "周内最高价")
    private BigDecimal weekHigh;
    
    /**
     * 年内最高价
     */
    @Excel(name = "年内最高价")
    private BigDecimal yearHigh;
    
    /**
     * 去年最高价
     */
    @Excel(name = "去年最高价")
    private BigDecimal lastYearHigh;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public void setYearLowPrice(BigDecimal yearLowPrice) {
        this.yearLowPrice = yearLowPrice;
    }

    public BigDecimal getYearLowPrice() {
        return yearLowPrice;
    }

    public void setYearLowPriceRate(BigDecimal yearLowPriceRate) {
        this.yearLowPriceRate = yearLowPriceRate;
    }

    public BigDecimal getYearLowPriceRate() {
        return yearLowPriceRate;
    }

    public void setThresholdPrice(BigDecimal thresholdPrice) {
        this.thresholdPrice = thresholdPrice;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }
    
    public BigDecimal getWeekHigh() {
        return weekHigh;
    }

    public void setWeekHigh(BigDecimal weekHigh) {
        this.weekHigh = weekHigh;
    }

    public BigDecimal getYearHigh() {
        return yearHigh;
    }

    public void setYearHigh(BigDecimal yearHigh) {
        this.yearHigh = yearHigh;
    }

    public BigDecimal getLastYearHigh() {
        return lastYearHigh;
    }

    public void setLastYearHigh(BigDecimal lastYearHigh) {
        this.lastYearHigh = lastYearHigh;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("date", getDate())
                .append("code", getCode())
                .append("name", getName())
                .append("newPrice", getNewPrice())
                .append("previousClose", getPreviousClose())
                .append("yearLowPrice", getYearLowPrice())
                .append("yearLowPriceRate", getYearLowPriceRate())
                .append("thresholdPrice", getThresholdPrice())
                .append("weekHigh", getWeekHigh())
                .append("yearHigh", getYearHigh())
                .append("lastYearHigh", getLastYearHigh())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}