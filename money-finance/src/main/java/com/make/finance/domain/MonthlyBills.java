package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 月度账单 (单JSON架构)对象 monthly_bills
 *
 * @author erqi
 * @date 2026-04-01
 */
public class MonthlyBills extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 账单主键 ID
     */
    private Long id;

    /**
     * 所属用户 ID
     */
    @Excel(name = "所属用户 ID")
    private Long userId;

    /**
     * 账单月份，格式 YYYY-MM
     */
    @Excel(name = "账单月份，格式 YYYY-MM")
    private String billMonth;

    /**
     * 当月总支出
     */
    @Excel(name = "当月总支出")
    private BigDecimal totalAmount;

    /**
     * 动态明细数据 (存储JSON数组)
     */
    @Excel(name = "动态明细数据 (存储JSON数组)")
    private String itemsData;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
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

    public void setBillMonth(String billMonth) {
        this.billMonth = billMonth;
    }

    public String getBillMonth() {
        return billMonth;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setItemsData(String itemsData) {
        this.itemsData = itemsData;
    }

    public String getItemsData() {
        return itemsData;
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
                .append("billMonth", getBillMonth())
                .append("totalAmount", getTotalAmount())
                .append("itemsData", getItemsData())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
