package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 买房支出记录对象 house_expense
 *
 * @author 贰柒
 * @date 2025-07-29
 */
public class HouseExpense extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    @Excel(name = "用户ID")
    private Long userId;

    /**
     * 房产名称（如小区名+楼号+房号）
     */
    @Excel(name = "房产名称", readConverterExp = "如=小区名+楼号+房号")
    private String houseName;

    /**
     * 房产地址
     */
    @Excel(name = "房产地址")
    private String location;

    /**
     * 房屋面积（平方米）
     */
    @Excel(name = "房屋面积", readConverterExp = "平=方米")
    private BigDecimal area;

    /**
     * 房屋总价（购房合同总金额）
     */
    @Excel(name = "房屋总价", readConverterExp = "购=房合同总金额")
    private BigDecimal totalPrice;

    /**
     * 支出类型（如首付、税费、装修等）
     */
    @Excel(name = "支出类型", readConverterExp = "如=首付、税费、装修等")
    private String expenseType;

    /**
     * 支出金额（单位：元）
     */
    @Excel(name = "支出金额", readConverterExp = "单=位：元")
    private BigDecimal amount;

    /**
     * 支出发生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支出发生日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date paymentDate;

    /**
     * 收款方（如开发商、中介、银行等）
     */
    @Excel(name = "收款方", readConverterExp = "如=开发商、中介、银行等")
    private String payee;

    /**
     * 买房阶段（购房/贷款/装修/其他）
     */
    @Excel(name = "买房阶段", readConverterExp = "购=房/贷款/装修/其他")
    private String stage;

    /**
     * 备注信息（如发票号、付款方式等）
     */
    @Excel(name = "备注信息", readConverterExp = "如=发票号、付款方式等")
    private String notes;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    /**
     * 创建人
     */
    @Excel(name = "创建人")
    private Long createdName;

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

    public void setHouseName(String houseName) {
        this.houseName = houseName;
    }

    public String getHouseName() {
        return houseName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public String getPayee() {
        return payee;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
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

    public void setCreatedName(Long createdName) {
        this.createdName = createdName;
    }

    public Long getCreatedName() {
        return createdName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("houseName", getHouseName())
                .append("location", getLocation())
                .append("area", getArea())
                .append("totalPrice", getTotalPrice())
                .append("expenseType", getExpenseType())
                .append("amount", getAmount())
                .append("paymentDate", getPaymentDate())
                .append("payee", getPayee())
                .append("stage", getStage())
                .append("notes", getNotes())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("createdName", getCreatedName())
                .toString();
    }
}
