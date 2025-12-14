package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 银行流水对象 bank_card_transactions
 *
 * @author erqi
 * @date 2025-05-27
 */
public class BankCardTransactions extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 账户号码
     */
    @Excel(name = "账户号码")
    private String AccountNo;

    /**
     * 分行
     */
    @Excel(name = "分行")
    private String SubBranch;

    /**
     * 交易日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date Date;

    /**
     * 货币类型
     */
    @Excel(name = "货币类型")
    private String Currency;

    /**
     * 交易详情
     */
    @Excel(name = "交易详情")
    private String Transaction;

    /**
     * 交易金额
     */
    @Excel(name = "交易金额")
    private BigDecimal Amount;

    /**
     * 归属银行
     */
    @Excel(name = "归属银行")
    private String bank;

    /**
     * 账户余额
     */
    @Excel(name = "账户余额")
    private BigDecimal Balance;

    /**
     * 交易类型
     */
    @Excel(name = "交易类型")
    private String TransactionType;

    /**
     * 交易对方
     */
    @Excel(name = "交易对方")
    private String CounterParty;

    /**
     * 备注
     */
    @Excel(name = "备注")
    private String note;
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

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setAccountNo(String AccountNo) {
        this.AccountNo = AccountNo;
    }

    public String getAccountNo() {
        return AccountNo;
    }

    public void setSubBranch(String SubBranch) {
        this.SubBranch = SubBranch;
    }

    public String getSubBranch() {
        return SubBranch;
    }

    public void setDate(Date Date) {
        this.Date = Date;
    }

    public Date getDate() {
        return Date;
    }

    public void setCurrency(String Currency) {
        this.Currency = Currency;
    }

    public String getCurrency() {
        return Currency;
    }

    public void setTransaction(String Transaction) {
        this.Transaction = Transaction;
    }

    public String getTransaction() {
        return Transaction;
    }

    public void setAmount(BigDecimal Amount) {
        this.Amount = Amount;
    }

    public BigDecimal getAmount() {
        return Amount;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getBank() {
        return bank;
    }

    public void setBalance(BigDecimal Balance) {
        this.Balance = Balance;
    }

    public BigDecimal getBalance() {
        return Balance;
    }

    public void setTransactionType(String TransactionType) {
        this.TransactionType = TransactionType;
    }

    public String getTransactionType() {
        return TransactionType;
    }

    public void setCounterParty(String CounterParty) {
        this.CounterParty = CounterParty;
    }

    public String getCounterParty() {
        return CounterParty;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("AccountNo", getAccountNo())
                .append("SubBranch", getSubBranch())
                .append("Date", getDate())
                .append("Currency", getCurrency())
                .append("Transaction", getTransaction())
                .append("Amount", getAmount())
                .append("bank", getBank())
                .append("Balance", getBalance())
                .append("TransactionType", getTransactionType())
                .append("CounterParty", getCounterParty())
                .append("note", getNote())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
