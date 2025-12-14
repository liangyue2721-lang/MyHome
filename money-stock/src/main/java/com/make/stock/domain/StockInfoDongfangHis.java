package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 东方财富历史对象 stock_info_dongfang_his
 *
 * @author erqi
 * @date 2025-05-28
 */
public class StockInfoDongfangHis extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 股票的唯一标识符，通常为股票的ID
     */
    private Long id;

    /**
     * 保存时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "保存时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date inDate;

    /**
     * 股票类型（如：2 代表股票，1 代表其他）
     */
    @Excel(name = "股票类型", readConverterExp = "如=：2,代=表股票，1,代=表其他")
    private BigDecimal type;

    /**
     * 股票当前价格
     */
    @Excel(name = "股票当前价格")
    private BigDecimal price;

    /**
     * 股票的交易量
     */
    @Excel(name = "股票的交易量")
    private BigDecimal volume;

    /**
     * 股票的市值，通常为股票价格 * 流通股数
     */
    @Excel(name = "股票的市值，通常为股票价格 * 流通股数")
    private BigDecimal marketValue;

    /**
     * 股票的总资产
     */
    @Excel(name = "股票的总资产")
    private BigDecimal totalAssets;

    /**
     * 股票的总股数
     */
    @Excel(name = "股票的总股数")
    private BigDecimal totalShares;

    /**
     * 股票的流通股数
     */
    @Excel(name = "股票的流通股数")
    private BigDecimal circulationShares;

    /**
     * 股票的涨跌额
     */
    @Excel(name = "股票的涨跌额")
    private BigDecimal netChange;

    /**
     * 股票的涨跌幅，通常以百分比显示
     */
    @Excel(name = "股票的涨跌幅，通常以百分比显示")
    private BigDecimal netChangePercentage;

    /**
     * 股票的代码
     */
    @Excel(name = "股票的代码")
    @JsonProperty("code")
    private String stockCode;

    /**
     * 股票所属市场类别
     */
    @Excel(name = "股票所属市场类别")
    private BigDecimal marketCategory;

    /**
     * 股票的公司名称
     */
    @Excel(name = "股票的公司名称")
    @JsonProperty("name")
    private String companyName;

    /**
     * 股票的最高价
     */
    @Excel(name = "股票的最高价")
    private BigDecimal highPrice;

    /**
     * 股票的最低价
     */
    @Excel(name = "股票的最低价")
    private BigDecimal lowPrice;

    /**
     * 股票的开盘价
     */
    @Excel(name = "股票的开盘价")
    private BigDecimal openPrice;

    /**
     * 股票的收盘价
     */
    @Excel(name = "股票的收盘价")
    private BigDecimal closePrice;

    /**
     * 股票的交易量
     */
    @Excel(name = "股票的交易量")
    private BigDecimal tradingVolume;

    /**
     * 附加信息字段（如某些特定的标记）
     */
    @Excel(name = "附加信息字段", readConverterExp = "如=某些特定的标记")
    private BigDecimal additionalInfo;

    // 覆盖父类的getParams方法，避免Map类型字段写入Excel时报错
    @Override
    public Map<String, Object> getParams() {
        return null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setInDate(Date inDate) {
        this.inDate = inDate;
    }

    public Date getInDate() {
        return inDate;
    }

    public void setType(BigDecimal type) {
        this.type = type;
    }

    public BigDecimal getType() {
        return type;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalShares(BigDecimal totalShares) {
        this.totalShares = totalShares;
    }

    public BigDecimal getTotalShares() {
        return totalShares;
    }

    public void setCirculationShares(BigDecimal circulationShares) {
        this.circulationShares = circulationShares;
    }

    public BigDecimal getCirculationShares() {
        return circulationShares;
    }

    public void setNetChange(BigDecimal netChange) {
        this.netChange = netChange;
    }

    public BigDecimal getNetChange() {
        return netChange;
    }

    public void setNetChangePercentage(BigDecimal netChangePercentage) {
        this.netChangePercentage = netChangePercentage;
    }

    public BigDecimal getNetChangePercentage() {
        return netChangePercentage;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setMarketCategory(BigDecimal marketCategory) {
        this.marketCategory = marketCategory;
    }

    public BigDecimal getMarketCategory() {
        return marketCategory;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setTradingVolume(BigDecimal tradingVolume) {
        this.tradingVolume = tradingVolume;
    }

    public BigDecimal getTradingVolume() {
        return tradingVolume;
    }

    public void setAdditionalInfo(BigDecimal additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public BigDecimal getAdditionalInfo() {
        return additionalInfo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("inDate", getInDate())
                .append("type", getType())
                .append("price", getPrice())
                .append("volume", getVolume())
                .append("marketValue", getMarketValue())
                .append("totalAssets", getTotalAssets())
                .append("totalShares", getTotalShares())
                .append("circulationShares", getCirculationShares())
                .append("netChange", getNetChange())
                .append("netChangePercentage", getNetChangePercentage())
                .append("stockCode", getStockCode())
                .append("marketCategory", getMarketCategory())
                .append("companyName", getCompanyName())
                .append("highPrice", getHighPrice())
                .append("lowPrice", getLowPrice())
                .append("openPrice", getOpenPrice())
                .append("closePrice", getClosePrice())
                .append("tradingVolume", getTradingVolume())
                .append("additionalInfo", getAdditionalInfo())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
