package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 攒金收益记录对象 gold_earnings
 *
 * @author erqi
 * @date 2025-05-28
 */
public class GoldEarnings extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    @Excel(name = "用户ID")
    private Long userId;

    /**
     * 买入克数
     */
    @Excel(name = "买入克数")
    private BigDecimal buyWeight;

    /**
     * 买入价格（元/克）
     */
    @Excel(name = "买入价格", readConverterExp = "元=/克")
    private BigDecimal buyPricePerGram;

    /**
     * 实时基准价（元/克）
     */
    @Excel(name = "实时基准价", readConverterExp = "元=/克")
    private BigDecimal benchmarkPrice;

    /**
     * 回收价（元/克）
     */
    @Excel(name = "回收价", readConverterExp = "元=/克")
    private BigDecimal recyclePrice;

    /**
     * 所属机构
     */
    @Excel(name = "所属机构")
    private String institution;

    /**
     * 收益金额（元）
     */
    @Excel(name = "收益金额", readConverterExp = "元=")
    private BigDecimal profitAmount;

    /**
     * 收益日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "收益日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date profitDate;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setBuyWeight(BigDecimal buyWeight) {
        this.buyWeight = buyWeight;
    }

    public BigDecimal getBuyWeight() {
        return buyWeight;
    }

    public void setBuyPricePerGram(BigDecimal buyPricePerGram) {
        this.buyPricePerGram = buyPricePerGram;
    }

    public BigDecimal getBuyPricePerGram() {
        return buyPricePerGram;
    }

    public void setBenchmarkPrice(BigDecimal benchmarkPrice) {
        this.benchmarkPrice = benchmarkPrice;
    }

    public BigDecimal getBenchmarkPrice() {
        return benchmarkPrice;
    }

    public void setRecyclePrice(BigDecimal recyclePrice) {
        this.recyclePrice = recyclePrice;
    }

    public BigDecimal getRecyclePrice() {
        return recyclePrice;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getInstitution() {
        return institution;
    }

    public void setProfitAmount(BigDecimal profitAmount) {
        this.profitAmount = profitAmount;
    }

    public BigDecimal getProfitAmount() {
        return profitAmount;
    }

    public void setProfitDate(Date profitDate) {
        this.profitDate = profitDate;
    }

    public Date getProfitDate() {
        return profitDate;
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
                .append("userId", getUserId())
                .append("buyWeight", getBuyWeight())
                .append("buyPricePerGram", getBuyPricePerGram())
                .append("benchmarkPrice", getBenchmarkPrice())
                .append("recyclePrice", getRecyclePrice())
                .append("institution", getInstitution())
                .append("profitAmount", getProfitAmount())
                .append("profitDate", getProfitDate())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
