package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 建行信用卡交易记录对象 cbc_credit_card_transaction
 *
 * @author 贰柒
 * @date 2025-05-26
 */
public class CbcCreditCardTransaction extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增
     */
    private Long id;

    /**
     * 交易发生日期，格式 yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易发生日期，格式 yyyy-MM-dd", width = 30, dateFormat = "yyyy-MM-dd")
    @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd")
    private Date tradeDate;

    /**
     * 交易入账日期，格式 yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易入账日期，格式 yyyy-MM-dd", width = 30, dateFormat = "yyyy-MM-dd")
    @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd")
    private Date postDate;

    /**
     * 信用卡尾号后四位
     */
    @Excel(name = "信用卡尾号后四位")
    private String cardLast4;

    /**
     * 交易描述信息
     */
    @Excel(name = "交易描述信息")
    private String description;

    /**
     * 交易原始金额，格式 币种/金额
     */
    @Excel(name = "交易原始金额，格式 币种/金额")
    private BigDecimal transAmount;

    /**
     * 结算金额，格式 币种/金额
     */
    @Excel(name = "结算金额，格式 币种/金额")
    private BigDecimal settleAmount;
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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setPostDate(Date postDate) {
        this.postDate = postDate;
    }

    public Date getPostDate() {
        return postDate;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setTransAmount(BigDecimal transAmount) {
        this.transAmount = transAmount;
    }

    public BigDecimal getTransAmount() {
        return transAmount;
    }

    public void setSettleAmount(BigDecimal settleAmount) {
        this.settleAmount = settleAmount;
    }

    public BigDecimal getSettleAmount() {
        return settleAmount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("tradeDate", getTradeDate())
                .append("postDate", getPostDate())
                .append("cardLast4", getCardLast4())
                .append("description", getDescription())
                .append("transAmount", getTransAmount())
                .append("settleAmount", getSettleAmount())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .append("userId", getUserId())
                .toString();
    }
}
