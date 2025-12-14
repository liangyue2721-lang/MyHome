package com.make.finance.domain.vo;


/**
 * 每月支入\支出柱状图 参数对象
 *
 * @author Devil
 */
public class MonthlyExpenditureBarChart {
    // 交易时间
    private String transactionTime;
    // 支入金额
    private double supportInAmount;
    // 支出金额
    private double supportOutAmount;
    // 结余金额
    private double balanceAmount;

    /**
     * 构造方法，初始化交易记录。
     *
     * @param transactionTime  交易时间
     * @param supportOutAmount 支出金额
     */
    public MonthlyExpenditureBarChart(String transactionTime, double supportOutAmount) {
        this.transactionTime = transactionTime;
        this.supportOutAmount = supportOutAmount;
    }

    public MonthlyExpenditureBarChart() {
    }

    /**
     * 构造方法，初始化交易记录。
     *
     * @param transactionTime  交易时间
     * @param supportInAmount  支入金额
     * @param supportOutAmount 支出金额
     */
    public MonthlyExpenditureBarChart(String transactionTime, double supportInAmount, double supportOutAmount) {
        this.transactionTime = transactionTime;
        this.supportInAmount = supportInAmount;
        this.supportOutAmount = supportOutAmount;
    }

    /**
     * 构造方法，初始化交易记录。
     *
     * @param transactionTime  交易时间
     * @param supportInAmount  支入金额
     * @param supportOutAmount 支出金额
     */
    public MonthlyExpenditureBarChart(String transactionTime, double supportInAmount, double supportOutAmount, double balanceAmount) {
        this.transactionTime = transactionTime;
        this.supportInAmount = supportInAmount;
        this.supportOutAmount = supportOutAmount;
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
    public double getSupportInAmount() {
        return supportInAmount;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public void setSupportInAmount(double supportInAmount) {
        this.supportInAmount = supportInAmount;
    }

    public double getSupportOutAmount() {
        return supportOutAmount;
    }

    public void setSupportOutAmount(double supportOutAmount) {
        this.supportOutAmount = supportOutAmount;
    }

    public double getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(double balanceAmount) {
        this.balanceAmount = balanceAmount;
    }
}
