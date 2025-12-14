package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 婚礼收支明细对象 wedding_transactions
 *
 * @author 贰柒
 * @date 2025-07-29
 */
public class WeddingTransactions extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 交易日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date transactionDate;

    /**
     * 交易类型
     */
    @Excel(name = "交易类型")
    private String transactionType;

    /**
     * 交易类别
     */
    @Excel(name = "交易类别")
    private String category;

    /**
     * 具体项目名称
     */
    @Excel(name = "具体项目名称")
    private String itemName;

    /**
     * 交易金额
     */
    @Excel(name = "交易金额")
    private BigDecimal amount;

    /**
     * 支付方式，如现金、银行卡等
     */
    @Excel(name = "支付方式，如现金、银行卡等")
    private String paymentMethod;

    /**
     * 参与人，如新郎、新娘、亲友等
     */
    @Excel(name = "参与人，如新郎、新娘、亲友等")
    private String participant;

    /**
     * 备注信息
     */
    @Excel(name = "备注信息")
    private String notes;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

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

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setParticipant(String participant) {
        this.participant = participant;
    }

    public String getParticipant() {
        return participant;
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
                .append("transactionDate", getTransactionDate())
                .append("transactionType", getTransactionType())
                .append("category", getCategory())
                .append("itemName", getItemName())
                .append("amount", getAmount())
                .append("paymentMethod", getPaymentMethod())
                .append("participant", getParticipant())
                .append("notes", getNotes())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("userId", getUserId())
                .toString();
    }
}
