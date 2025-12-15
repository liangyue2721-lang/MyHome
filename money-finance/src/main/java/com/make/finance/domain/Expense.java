package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 消费对象 expense
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class Expense extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 消费ID
     */
    private Long expenseId;

    /**
     * 关联用户
     */
    @Excel(name = "关联用户")
    private Long userId;

    /**
     * 消费金额
     */
    @Excel(name = "消费金额")
    private BigDecimal amount;

    /**
     * 商户名称
     */
    @Excel(name = "商户名称")
    private String merchant;

    /**
     * 消费日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "消费日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expenseDate;

    /**
     * 消费分类
     */
    @Excel(name = "消费分类")
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

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }

    public Long getExpenseId() {
        return expenseId;
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

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setExpenseDate(Date expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Date getExpenseDate() {
        return expenseDate;
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
                .append("expenseId", getExpenseId())
                .append("userId", getUserId())
                .append("amount", getAmount())
                .append("merchant", getMerchant())
                .append("expenseDate", getExpenseDate())
                .append("category", getCategory())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
