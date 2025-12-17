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
     * 婚礼名称
     */
    @Excel(name = "婚礼名称")
    private String weddingName;

    /**
     * 婚礼日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "婚礼日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date weddingDate;

    /**
     * 婚礼举办城市
     */
    @Excel(name = "婚礼举办城市")
    private String weddingCity;

    /**
     * 支出分类（酒席/婚纱/摄影/主持/场地/礼品/其他）
     */
    @Excel(name = "支出分类", readConverterExp = "酒=席/婚纱/摄影/主持/场地/礼品/其他")
    private String expenseCategory;

    /**
     * 具体支出项目
     */
    @Excel(name = "具体支出项目")
    private String expenseItem;

    /**
     * 支出金额（元）
     */
    @Excel(name = "支出金额", readConverterExp = "支出金额（元）")
    private BigDecimal amount;

    /**
     * 支付日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date paymentDate;

    /**
     * 收款方
     */
    @Excel(name = "收款方")
    private String payee;

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

    public void setWeddingName(String weddingName) {
        this.weddingName = weddingName;
    }

    public String getWeddingName() {
        return weddingName;
    }

    public void setWeddingDate(Date weddingDate) {
        this.weddingDate = weddingDate;
    }

    public Date getWeddingDate() {
        return weddingDate;
    }

    public void setWeddingCity(String weddingCity) {
        this.weddingCity = weddingCity;
    }

    public String getWeddingCity() {
        return weddingCity;
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

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public String getPayee() {
        return payee;
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
                .append("weddingName", getWeddingName())
                .append("weddingDate", getWeddingDate())
                .append("weddingCity", getWeddingCity())
                .append("expenseCategory", getExpenseCategory())
                .append("expenseItem", getExpenseItem())
                .append("amount", getAmount())
                .append("paymentDate", getPaymentDate())
                .append("payee", getPayee())
                .append("notes", getNotes())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("createdBy", getCreatedBy())
                .toString();
    }
}
