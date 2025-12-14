package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 收入对象 income
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public class Income extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 收入ID
     */
    private Long incomeId;

    /**
     * 关联用户
     */
    @Excel(name = "关联用户")
    private Long userId;

    /**
     * 金额（支持百万级）
     */
    @Excel(name = "金额", readConverterExp = "支=持百万级")
    private BigDecimal amount;

    /**
     * 收入来源
     */
    @Excel(name = "收入来源")
    private String source;

    /**
     * 收入日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "收入日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date incomeDate;

    /**
     * 收入分类
     */
    @Excel(name = "收入分类")
    private String category;

    /**
     * 记录时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;


    private Date startDate;
    private Date endDate;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setIncomeId(Long incomeId) {
        this.incomeId = incomeId;
    }

    public Long getIncomeId() {
        return incomeId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setIncomeDate(Date incomeDate) {
        this.incomeDate = incomeDate;
    }

    public Date getIncomeDate() {
        return incomeDate;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
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
                .append("incomeId", getIncomeId())
                .append("userId", getUserId())
                .append("amount", getAmount())
                .append("source", getSource())
                .append("incomeDate", getIncomeDate())
                .append("category", getCategory())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
