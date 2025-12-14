package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 微信支付宝流水对象 transaction_records
 *
 * @author è´°æ
 * @date 2025-07-29
 */
public class TransactionRecords extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Long id;

    /**
     * 交易时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date transactionTime;

    /**
     * 交易类型
     */
    @Excel(name = "交易类型")
    private String transactionType;

    /**
     * 交易对方
     */
    @Excel(name = "交易对方")
    private String counterparty;

    /**
     * 对方账号
     */
    @Excel(name = "对方账号")
    private String counterpartyAccount;

    /**
     * 商品
     */
    @Excel(name = "商品")
    private String product;

    /**
     * 商品类型
     */
    @Excel(name = "商品类型")
    private String productType;

    /**
     * 流水来源
     */
    @Excel(name = "流水来源")
    private String source;

    /**
     * 收入/支出
     */
    @Excel(name = "收入/支出")
    private String inOut;

    /**
     * 金额(元)
     */
    @Excel(name = "金额(元)")
    private BigDecimal amount;

    /**
     * 支付方式
     */
    @Excel(name = "支付方式")
    private String paymentMethod;

    /**
     * 当前状态
     */
    private String transactionStatus;

    /**
     * 交易单号
     */
    @Excel(name = "交易单号")
    private String transactionId;

    /**
     * 商户单号
     */
    @Excel(name = "商户单号")
    private String merchantId;

    /**
     * 备注
     */
    @Excel(name = "备注")
    private String note;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
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

    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Date getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterpartyAccount(String counterpartyAccount) {
        this.counterpartyAccount = counterpartyAccount;
    }

    public String getCounterpartyAccount() {
        return counterpartyAccount;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductType() {
        return productType;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setInOut(String inOut) {
        this.inOut = inOut;
    }

    public String getInOut() {
        return inOut;
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

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
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
                .append("transactionTime", getTransactionTime())
                .append("transactionType", getTransactionType())
                .append("counterparty", getCounterparty())
                .append("counterpartyAccount", getCounterpartyAccount())
                .append("product", getProduct())
                .append("productType", getProductType())
                .append("source", getSource())
                .append("inOut", getInOut())
                .append("amount", getAmount())
                .append("paymentMethod", getPaymentMethod())
                .append("transactionStatus", getTransactionStatus())
                .append("transactionId", getTransactionId())
                .append("merchantId", getMerchantId())
                .append("note", getNote())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("userId", getUserId())
                .toString();
    }
}
