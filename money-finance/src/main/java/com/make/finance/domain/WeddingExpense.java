package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 婚礼支出记录对象 wedding_expense
 *
 * @author erqi
 * @date 2025-12-17
 */
public class WeddingExpense extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支出分类
     */
    @Excel(name = "支出分类")
    private String expenseCategory;

    /**
     * 具体支出项目
     */
    @Excel(name = "具体支出项目")
    private String expenseItem;

    /**
     * 支出金额（元）
     */
    @Excel(name = "支出金额")
    private BigDecimal amount;

    /**
     * 支付日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date paymentDate;

    /**
     * 婚礼阶段
     */
    @Excel(name = "婚礼阶段")
    private Integer stage;

    /**
     * 出资方归属
     */
    @Excel(name = "出资方归属")
    private Integer payerType;

    /**
     * 付款人
     */
    @Excel(name = "付款人")
    private String payer;

    /**
     * 备注说明
     */
    @Excel(name = "备注说明")
    private String notes;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    /**
     * 创建人
     */
    private Long createdBy;

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

    public void setExpenseCategory(String expenseCategory) {
        this.expenseCategory = expenseCategory;
    }

    public String getExpenseCategory() {
        return expenseCategory;
    }

    public void setExpenseItem(String expenseItem) {
        this.expenseItem = expenseItem;
    }

    public String getExpenseItem() {
        return expenseItem;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getPayerType() {
        return payerType;
    }

    public void setPayerType(Integer payerType) {
        this.payerType = payerType;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getPayer() {
        return payer;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
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

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("expenseCategory", getExpenseCategory())
                .append("expenseItem", getExpenseItem())
                .append("amount", getAmount())
                .append("paymentDate", getPaymentDate())
                .append("stage", getStage())
                .append("payerType", getPayerType())
                .append("payer", getPayer())
                .append("notes", getNotes())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("createdBy", getCreatedBy())
                .toString();
    }
}
