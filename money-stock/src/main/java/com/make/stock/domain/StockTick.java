package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 股票逐笔成交明细对象 stock_tick
 *
 * @author erqi
 * @date 2026-01-27
 */
public class StockTick extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 股票代码
     */
    @Excel(name = "股票代码")
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
     * 买卖方向 (1:买入, 2:卖出, 4:中性)
     */
    @Excel(name = "买卖方向 (1:买入, 2:卖出, 4:中性)")
    private Long sideCode;

    /**
     * 该时刻包含的成交笔数
     */
    @Excel(name = "该时刻包含的成交笔数")
    private Long tickCount;

    /**
     * 每笔平均成交量
     */
    @Excel(name = "每笔平均成交量")
    private BigDecimal avgVol;

    /**
     * 是否判定为大资金入场 (0:否, 1:是)
     */
    @Excel(name = "是否判定为大资金入场 (0:否, 1:是)")
    private Integer isBigMoney;

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

    public void setSideCode(Long sideCode) {
        this.sideCode = sideCode;
    }

    public Long getSideCode() {
        return sideCode;
    }

    public void setTickCount(Long tickCount) {
        this.tickCount = tickCount;
    }

    public Long getTickCount() {
        return tickCount;
    }

    public void setAvgVol(BigDecimal avgVol) {
        this.avgVol = avgVol;
    }

    public BigDecimal getAvgVol() {
        return avgVol;
    }

    public void setIsBigMoney(Integer isBigMoney) {
        this.isBigMoney = isBigMoney;
    }

    public Integer getIsBigMoney() {
        return isBigMoney;
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
                .append("sideCode", getSideCode())
                .append("tickCount", getTickCount())
                .append("avgVol", getAvgVol())
                .append("isBigMoney", getIsBigMoney())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
