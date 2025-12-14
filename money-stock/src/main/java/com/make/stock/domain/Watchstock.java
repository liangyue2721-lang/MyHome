package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 买入价位提醒对象 watchstock
 *
 * @author erqi
 * @date 2025-05-28
 */
public class Watchstock extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
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
     * 涨跌幅
     */
    @Excel(name = "涨跌幅")
    private BigDecimal changeRate;

    /**
     * 涨跌额
     */
    @Excel(name = "涨跌额")
    private BigDecimal upsDowns;

    /**
     * 昨收
     */
    @Excel(name = "昨收")
    private BigDecimal previousClose;

    /**
     * 阈值价格
     */
    @Excel(name = "阈值价格")
    private BigDecimal thresholdPrice;

    /**
     * api接口
     */
    @Excel(name = "api接口")
    private String stockApi;

    /**
     * 发送次数
     */
    @Excel(name = "发送次数")
    private Integer num;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /**
     * 今年最低价位
     */
    @Excel(name = "今年最低价位")
    private BigDecimal yearLow;

    /**
     * 与今年最低价位比较
     */
    @Excel(name = "与今年最低价位比较")
    private BigDecimal compareYearLow;
    
    /**
     * 周内最低价位
     */
    @Excel(name = "周内最低价位")
    private BigDecimal weekLow;
    
    /**
     * 今年最高
     */
    @Excel(name = "今年最高")
    private BigDecimal yearHigh;
    
    /**
     * 去年最高
     */
    @Excel(name = "去年最高")
    private BigDecimal compareYearHigh;
    
    /**
     * 周内最高
     */
    @Excel(name = "周内最高")
    private BigDecimal weekHigh;


    /**
     * 最高价
     */
    @Excel(name = "当日最高")
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    @Excel(name = "当日最低")
    private BigDecimal lowPrice;

    // -------------------- Setter 方法（链式） --------------------

    public Watchstock setId(Long id) {
        this.id = id;
        return this;
    }

    public Watchstock setDate(Date date) {
        this.date = date;
        return this;
    }

    public Watchstock setCode(String code) {
        this.code = code;
        return this;
    }

    public Watchstock setName(String name) {
        this.name = name;
        return this;
    }

    public Watchstock setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
        return this;
    }

    public Watchstock setChangeRate(BigDecimal changeRate) {
        this.changeRate = changeRate;
        return this;
    }

    public Watchstock setUpsDowns(BigDecimal upsDowns) {
        this.upsDowns = upsDowns;
        return this;
    }

    public Watchstock setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
        return this;
    }

    public Watchstock setThresholdPrice(BigDecimal thresholdPrice) {
        this.thresholdPrice = thresholdPrice;
        return this;
    }

    public Watchstock setStockApi(String stockApi) {
        this.stockApi = stockApi;
        return this;
    }

    public Watchstock setNum(Integer num) {
        this.num = num;
        return this;
    }

    public Watchstock setYearLow(BigDecimal yearLow) {
        this.yearLow = yearLow;
        return this;
    }

    public Watchstock setCompareYearLow(BigDecimal compareYearLow) {
        this.compareYearLow = compareYearLow;
        return this;
    }
    
    public Watchstock setWeekLow(BigDecimal weekLow) {
        this.weekLow = weekLow;
        return this;
    }
    
    public Watchstock setYearHigh(BigDecimal yearHigh) {
        this.yearHigh = yearHigh;
        return this;
    }
    
    public Watchstock setCompareYearHigh(BigDecimal compareYearHigh) {
        this.compareYearHigh = compareYearHigh;
        return this;
    }
    
    public Watchstock setWeekHigh(BigDecimal weekHigh) {
        this.weekHigh = weekHigh;
        return this;
    }

    public Watchstock setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
        return this;
    }

    public Watchstock setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
        return this;
    }

    // -------------------- Getter 方法 --------------------

    public Long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public BigDecimal getChangeRate() {
        return changeRate;
    }

    public BigDecimal getUpsDowns() {
        return upsDowns;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public BigDecimal getThresholdPrice() {
        return thresholdPrice;
    }

    public String getStockApi() {
        return stockApi;
    }

    public Integer getNum() {
        return num;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getYearLow() {
        return yearLow;
    }

    public BigDecimal getCompareYearLow() {
        return compareYearLow;
    }
    
    public BigDecimal getWeekLow() {
        return weekLow;
    }
    
    public BigDecimal getYearHigh() {
        return yearHigh;
    }
    
    public BigDecimal getCompareYearHigh() {
        return compareYearHigh;
    }
    
    public BigDecimal getWeekHigh() {
        return weekHigh;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("date", getDate())
                .append("code", getCode())
                .append("name", getName())
                .append("newPrice", getNewPrice())
                .append("changeRate", getChangeRate())
                .append("upsDowns", getUpsDowns())
                .append("previousClose", getPreviousClose())
                .append("thresholdPrice", getThresholdPrice())
                .append("stockApi", getStockApi())
                .append("num", getNum())
                .append("updatedAt", getUpdatedAt())
                .append("yearLow", getYearLow())
                .append("compareYearLow", getCompareYearLow())
                .append("weekLow", getWeekLow())
                .append("yearHigh", getYearHigh())
                .append("compareYearHigh", getCompareYearHigh())
                .append("weekHigh", getWeekHigh())
                .toString();
    }
}