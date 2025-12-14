package com.make.finance.domain.vo;

/**
 * 表示贷款总额加利息偿还饼形图的数据项。
 * @author 84522
 */
public class LoanTotalWithInterestRepaymentPieChart {

    /**
     * 类别
     */
    private String category;
    /**
     * 金额
     */
    private double amount;

    public LoanTotalWithInterestRepaymentPieChart() {

    }

    /**
     * 构造具有给定类别和金额的 LoanTotalWithInterestRepaymentPieChart 对象。
     *
     * @param category 数据项的类别。
     * @param amount   与类别相关联的金额。
     */
    public LoanTotalWithInterestRepaymentPieChart(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
