package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;

/**
 * 投资利润回报记录对象 investment_records
 * 
 * @author make
 * @date 2025-08-23
 */
public class InvestmentRecords extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 成交日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "成交日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeDate;

    /** 成交价位 */
    @Excel(name = "成交价位")
    private BigDecimal tradePrice;

    /** 成交量（股/份） */
    @Excel(name = "成交量（股/份）")
    private Long tradeVolume;

    /** 成交金额 = 成交价位 * 成交量 */
    @Excel(name = "成交金额")
    private BigDecimal tradeAmount;

    /** 成交类型，如：买入、卖出 */
    @Excel(name = "成交类型", readConverterExp = "买=入,卖=出")
    private String tradeType;

    /** 投资方式，如：一次性投资、定投（可选） */
    @Excel(name = "投资方式，如：一次性投资、定投（可选）")
    private String investType;

    /** 本期投入本金金额 */
    @Excel(name = "本期投入本金金额")
    private BigDecimal principalAmount;

    /** 当期总资产 */
    @Excel(name = "当期总资产")
    private BigDecimal totalAsset;

    /** 本期收益（相较上期） */
    @Excel(name = "本期收益", readConverterExp = "相=较上期")
    private BigDecimal periodProfit;

    /** 累计收益（相较总投入） */
    @Excel(name = "累计收益", readConverterExp = "相=较总投入")
    private BigDecimal totalProfit;

    /** 当前收益率（%） */
    @Excel(name = "当前收益率", readConverterExp = "%=")
    private BigDecimal profitRate;

    /** 目标达成率（相对于100万）% */
    @Excel(name = "目标达成率", readConverterExp = "相=对于100万")
    private BigDecimal targetProgress;

    /** 备注信息 */
    @Excel(name = "备注信息")
    private String remark;

    /** 用户 ID，关联用户表 */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setTradeDate(Date tradeDate) 
    {
        this.tradeDate = tradeDate;
    }

    public Date getTradeDate() 
    {
        return tradeDate;
    }
    public void setTradePrice(BigDecimal tradePrice) 
    {
        this.tradePrice = tradePrice;
    }

    public BigDecimal getTradePrice() 
    {
        return tradePrice;
    }
    public void setTradeVolume(Long tradeVolume) 
    {
        this.tradeVolume = tradeVolume;
    }

    public Long getTradeVolume() 
    {
        return tradeVolume;
    }
    public void setTradeAmount(BigDecimal tradeAmount) 
    {
        this.tradeAmount = tradeAmount;
    }

    public BigDecimal getTradeAmount() 
    {
        return tradeAmount;
    }
    public void setTradeType(String tradeType) 
    {
        this.tradeType = tradeType;
    }

    public String getTradeType() 
    {
        return tradeType;
    }
    public void setInvestType(String investType) 
    {
        this.investType = investType;
    }

    public String getInvestType() 
    {
        return investType;
    }
    public void setPrincipalAmount(BigDecimal principalAmount) 
    {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getPrincipalAmount() 
    {
        return principalAmount;
    }
    public void setTotalAsset(BigDecimal totalAsset) 
    {
        this.totalAsset = totalAsset;
    }

    public BigDecimal getTotalAsset() 
    {
        return totalAsset;
    }
    public void setPeriodProfit(BigDecimal periodProfit) 
    {
        this.periodProfit = periodProfit;
    }

    public BigDecimal getPeriodProfit() 
    {
        return periodProfit;
    }
    public void setTotalProfit(BigDecimal totalProfit) 
    {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getTotalProfit() 
    {
        return totalProfit;
    }
    public void setProfitRate(BigDecimal profitRate) 
    {
        this.profitRate = profitRate;
    }

    public BigDecimal getProfitRate() 
    {
        return profitRate;
    }
    public void setTargetProgress(BigDecimal targetProgress) 
    {
        this.targetProgress = targetProgress;
    }

    public BigDecimal getTargetProgress() 
    {
        return targetProgress;
    }
    public void setRemark(String remark) 
    {
        this.remark = remark;
    }

    public String getRemark() 
    {
        return remark;
    }
    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("tradeDate", getTradeDate())
            .append("tradePrice", getTradePrice())
            .append("tradeVolume", getTradeVolume())
            .append("tradeAmount", getTradeAmount())
            .append("tradeType", getTradeType())
            .append("investType", getInvestType())
            .append("principalAmount", getPrincipalAmount())
            .append("totalAsset", getTotalAsset())
            .append("periodProfit", getPeriodProfit())
            .append("totalProfit", getTotalProfit())
            .append("profitRate", getProfitRate())
            .append("targetProgress", getTargetProgress())
            .append("remark", getRemark())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("userId", getUserId())
            .toString();
    }
}