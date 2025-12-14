package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户水电费缴纳记录对象 utility_payments
 *
 * @author 贰柒
 * @date 2025-05-27
 */
public class UtilityPayments extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Integer id;

    /**
     * 用户 ID，关联用户表
     */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;

    /**
     * 费用（单位：元）
     */
    @Excel(name = "费用", readConverterExp = "单=位：元")
    private BigDecimal fee;

    /**
     * 缴费类型（0=水费, 1=电费, 2=燃气费）
     */
    @Excel(name = "缴费类型", readConverterExp = "0==水费,,1==电费,,2==燃气费")
    private Integer paymentType;

    /**
     * 缴费状态（0=未缴, 1=已缴）
     */
    @Excel(name = "缴费状态", readConverterExp = "0==未缴,,1==已缴")
    private Integer paymentStatus;

    /**
     * 缴费日期（已缴费时记录）
     */
    @Excel(name = "缴费日期", readConverterExp = "已=缴费时记录")
    private Date paymentDate;

    /**
     * 记录创建时间
     */
    private Date createdAt;
    /**
     * 开始时间
     */
    private Date startDate;
    /**
     * 结束时间
     */
    private Date endDate;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "最后更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setPaymentType(Integer paymentType) {
        this.paymentType = paymentType;
    }

    public Integer getPaymentType() {
        return paymentType;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Date getPaymentDate() {
        return paymentDate;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("fee", getFee())
                .append("paymentType", getPaymentType())
                .append("paymentStatus", getPaymentStatus())
                .append("paymentDate", getPaymentDate())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
