package com.make.finance.domain;

import java.math.BigDecimal;

import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 年度投资汇总对象 yearly_investment_summary
 *
 * @author erqi
 * @date 2025-07-29
 */
public class YearlyInvestmentSummary extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 年份，例如 2025
     */
    @Excel(name = "年份，例如 2025")
    private Long year;

    /**
     * 年初本金，单位：元
     */
    @Excel(name = "年初本金，单位：元")
    private BigDecimal startPrincipal;

    /**
     * 年末期望增值率，单位：百分比
     */
    @Excel(name = "年末期望增值率，单位：百分比")
    private BigDecimal expectedGrowthRate;

    /**
     * 年末预期总值，单位：元
     */
    @Excel(name = "年末预期总值，单位：元")
    private BigDecimal expectedEndValue;

    /**
     * 年末实际增值率，单位：百分比
     */
    @Excel(name = "年末实际增值率，单位：百分比")
    private BigDecimal actualGrowthRate;

    /**
     * 年末实际总值，单位：元
     */
    @Excel(name = "年末实际总值，单位：元")
    private BigDecimal actualEndValue;

    /**
     * 是否完成，N=否，Y=是
     */
    @Excel(name = "是否完成，N=否，Y=是")
    private String isCompleted;

    /**
     * 用户 ID，关联用户表
     */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setYear(Long year) {
        this.year = year;
    }

    public Long getYear() {
        return year;
    }

    public void setStartPrincipal(BigDecimal startPrincipal) {
        this.startPrincipal = startPrincipal;
    }

    public BigDecimal getStartPrincipal() {
        return startPrincipal;
    }

    public void setExpectedGrowthRate(BigDecimal expectedGrowthRate) {
        this.expectedGrowthRate = expectedGrowthRate;
    }

    public BigDecimal getExpectedGrowthRate() {
        return expectedGrowthRate;
    }

    public void setExpectedEndValue(BigDecimal expectedEndValue) {
        this.expectedEndValue = expectedEndValue;
    }

    public BigDecimal getExpectedEndValue() {
        return expectedEndValue;
    }

    public void setActualGrowthRate(BigDecimal actualGrowthRate) {
        this.actualGrowthRate = actualGrowthRate;
    }

    public BigDecimal getActualGrowthRate() {
        return actualGrowthRate;
    }

    public void setActualEndValue(BigDecimal actualEndValue) {
        this.actualEndValue = actualEndValue;
    }

    public BigDecimal getActualEndValue() {
        return actualEndValue;
    }

    public void setIsCompleted(String isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getIsCompleted() {
        return isCompleted;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("year", getYear())
                .append("startPrincipal", getStartPrincipal())
                .append("expectedGrowthRate", getExpectedGrowthRate())
                .append("expectedEndValue", getExpectedEndValue())
                .append("actualGrowthRate", getActualGrowthRate())
                .append("actualEndValue", getActualEndValue())
                .append("isCompleted", getIsCompleted())
                .append("remark", getRemark())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("userId", getUserId())
                .toString();
    }
}
