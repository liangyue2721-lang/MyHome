package com.make.finance.domain.dto;

/**
 * 支付宝交易对象
 */
public class AliPayment {

    /**
     * 交易时间戳。
     */
    private String transactionTime;

    /**
     * 交易类型。
     */
    private String transactionType;

    /**
     * 交易对方。
     */
    private String counterparty;

    /**
     * 对方账号。
     */
    private String counterpartyAccount;

    /**
     * 商品说明。
     */
    private String productDescription;

    /**
     * 收支情况。
     */
    private String inOut;

    /**
     * 交易金额。
     */
    private String amount;

    /**
     * 支付方式。
     */
    private String paymentMethod;

    /**
     * 交易状态。
     */
    private String transactionStatus;

    /**
     * 交易订单号。
     */
    private String transactionOrderId;

    /**
     * 商家订单号。
     */
    private String merchantOrderId;

    /**
     * 备注。
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

    public String getCounterpartyAccount() {
        return counterpartyAccount;
    }

    public void setCounterpartyAccount(String counterpartyAccount) {
        this.counterpartyAccount = counterpartyAccount;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
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

    public String getTransactionOrderId() {
        return transactionOrderId;
    }

    public void setTransactionOrderId(String transactionOrderId) {
        this.transactionOrderId = transactionOrderId;
    }

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public void setMerchantOrderId(String merchantOrderId) {
        this.merchantOrderId = merchantOrderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
