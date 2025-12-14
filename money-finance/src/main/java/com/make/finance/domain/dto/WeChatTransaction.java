package com.make.finance.domain.dto;

/**
 * 微信账单流水文件
 *
 * @author 84522
 */
public class WeChatTransaction {

    /**
     * 交易时间
     */
    private String transactionTime;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易对方
     */
    private String counterparty;

    /**
     * 商品
     */
    private String product;

    /**
     * 收入/支出
     */
    private String inOut;

    /**
     * 金额(元)
     */
    private String amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 当前状态
     */
    private String transactionStatus;

    /**
     * 交易单号
     */
    private String transactionId;

    /**
     * 商户单号
     */
    private String merchantId;

    /**
     * 备注
     */
    private String note;

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getInOut() {
        return inOut;
    }

    public void setInOut(String inOut) {
        this.inOut = inOut;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
