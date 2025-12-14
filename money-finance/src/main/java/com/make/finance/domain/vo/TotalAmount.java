package com.make.finance.domain.vo;


public class TotalAmount {
    // 交易时间
    private String transactionTime;
    // 交易金额
    private double amount;

    public TotalAmount() {}
    /**
     * 构造方法，初始化交易记录。
     *
     * @param transactionTime 交易时间
     * @param amount          交易金额
     */
    public TotalAmount(String transactionTime, double amount) {
        this.transactionTime = transactionTime;
        this.amount = amount;
    }

    /**
     * 获取交易时间。
     *
     * @return 交易时间
     */
    public String getTransactionTime() {
        return transactionTime;
    }

    /**
     * 获取交易金额。
     *
     * @return 交易金额
     */
    public double getAmount() {
        return amount;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
