package com.make.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 贷款剩余计算对象 loan_repayments
 *
 * @author 贰柒
 * @date 2025-07-29
 */
public class LoanRepayments extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 贷款总额
     */
    @Excel(name = "贷款总额")
    private BigDecimal totalAmount;

    /**
     * 期数
     */
    @Excel(name = "期数")
    private Long installments;

    /**
     * 年龄
     */
    @Excel(name = "年龄")
    private int repaymentAge;

    /**
     * 还款日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "还款日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date repaymentDate;

    /**
     * 应还本金
     */
    @Excel(name = "应还本金")
    private BigDecimal principal;

    /**
     * 应还利息
     */
    @Excel(name = "应还利息")
    private BigDecimal interest;

    /**
     * 本息合计
     */
    @Excel(name = "本息合计")
    private BigDecimal totalPrincipalAndInterest;

    /**
     * 是否结清
     */
    @Excel(name = "是否结清")
    private Integer isSettled;

    /**
     * 浮动利率(LPR-50)
     */
    @Excel(name = "浮动利率(LPR-50)")
    private BigDecimal floatingInterestRate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") // 序列化和反序列化
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Spring MVC 参数绑定
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") // 序列化和反序列化
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Spring MVC 参数绑定
    private Date endDate;

    /**
     * 用户 ID，关联用户表
     */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public LoanRepayments() {
    }


    /**
     * 使用提供的详细信息构造一个新的LoanRepayments对象。
     *
     * @param totalAmount          贷款总额
     * @param term                 期数
     * @param paymentDate          还款日期
     * @param principal            应还本金
     * @param interest             应还利息
     * @param totalPayment         本息合计
     * @param floatingInterestRate 浮动利率(LPR-50)
     */
    public LoanRepayments(BigDecimal totalAmount, Long term, LocalDate paymentDate, BigDecimal principal,
                          BigDecimal interest, BigDecimal totalPayment, BigDecimal floatingInterestRate) {
        this.totalAmount = totalAmount;
        this.installments = term;
        // 将LocalDate转换为Date
        this.repaymentDate = java.sql.Date.valueOf(paymentDate);
        this.principal = principal;
        this.interest = interest;
        this.totalPrincipalAndInterest = totalPayment;
        this.floatingInterestRate = floatingInterestRate;
    }

    /**
     * 使用提供的详细信息构造一个新的LoanRepayments对象。
     *
     * @param term         期数
     * @param paymentDate  还款日期
     * @param principal    应还本金
     * @param interest     应还利息
     * @param totalPayment 本息合计
     */
    public LoanRepayments(Long term, LocalDate paymentDate, BigDecimal principal,
                          BigDecimal interest, BigDecimal totalPayment) {
        this.installments = term;
        // 将LocalDate转换为Date
        this.repaymentDate = java.sql.Date.valueOf(paymentDate);
        this.principal = principal;
        this.interest = interest;
        this.totalPrincipalAndInterest = totalPayment;
    }

    public int getRepaymentAge() {
        return repaymentAge;
    }

    public void setRepaymentAge(int repaymentAge) {
        this.repaymentAge = repaymentAge;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setInstallments(Long installments) {
        this.installments = installments;
    }

    public Long getInstallments() {
        return installments;
    }

    public void setRepaymentDate(Date repaymentDate) {
        this.repaymentDate = repaymentDate;
    }

    public Date getRepaymentDate() {
        return repaymentDate;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setInterest(BigDecimal interest) {
        this.interest = interest;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    public void setTotalPrincipalAndInterest(BigDecimal totalPrincipalAndInterest) {
        this.totalPrincipalAndInterest = totalPrincipalAndInterest;
    }

    public BigDecimal getTotalPrincipalAndInterest() {
        return totalPrincipalAndInterest;
    }

    public void setIsSettled(Integer isSettled) {
        this.isSettled = isSettled;
    }

    public Integer getIsSettled() {
        return isSettled;
    }

    public void setFloatingInterestRate(BigDecimal floatingInterestRate) {
        this.floatingInterestRate = floatingInterestRate;
    }

    public BigDecimal getFloatingInterestRate() {
        return floatingInterestRate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("totalAmount", getTotalAmount())
                .append("installments", getInstallments())
                .append("repaymentDate", getRepaymentDate())
                .append("principal", getPrincipal())
                .append("interest", getInterest())
                .append("totalPrincipalAndInterest", getTotalPrincipalAndInterest())
                .append("isSettled", getIsSettled())
                .append("floatingInterestRate", getFloatingInterestRate())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("userId", getUserId())
                .toString();
    }
}
