package com.make.finance.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户账户银行卡信息对象 user_accounts
 *
 * @author erqi
 * @date 2025-06-03
 */
public class UserAccounts extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户名ID，唯一
     */
    @Excel(name = "用户名ID，唯一")
    private Long userId;

    /**
     * 银行卡号
     */
    @Excel(name = "银行卡号")
    private String bankCardNumber;

    /**
     * 银行卡状态
     */
    @Excel(name = "银行卡状态")
    private String bankCardStatus;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setBankCardNumber(String bankCardNumber) {
        this.bankCardNumber = bankCardNumber;
    }

    public String getBankCardNumber() {
        return bankCardNumber;
    }

    public void setBankCardStatus(String bankCardStatus) {
        this.bankCardStatus = bankCardStatus;
    }

    public String getBankCardStatus() {
        return bankCardStatus;
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
                .append("userId", getUserId())
                .append("bankCardNumber", getBankCardNumber())
                .append("bankCardStatus", getBankCardStatus())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
