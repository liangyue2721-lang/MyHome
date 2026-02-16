package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 婚礼订婚支出流水对象 wedding_expenses
 *
 * @author erqi
 * @date 2026-02-16
 */
public class WeddingExpenses extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 项目名称：如 订婚宴、三金、婚庆尾款
     */
    @Excel(name = "项目名称：如 订婚宴、三金、婚庆尾款")
    private String itemName;

    /**
     * 金额：精确到分
     */
    @Excel(name = "金额：精确到分")
    private BigDecimal amount;

    /**
     * 消费日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "消费日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expenseDate;

    /**
     * 所属阶段：0=订婚, 1=婚礼, 2=婚后
     */
    @Excel(name = "所属阶段：0=订婚, 1=婚礼, 2=婚后")
    private String stage;

    /**
     * 出资方归属：0=小家, 1=男方父母, 2=女方父母
     */
    @Excel(name = "出资方归属：0=小家, 1=男方父母, 2=女方父母")
    private String payerType;

    /**
     * 分类：餐饮、珠宝、婚庆、交通、红包 (建议前端做成下拉选)
     */
    @Excel(name = "分类：餐饮、珠宝、婚庆、交通、红包 (建议前端做成下拉选)")
    private String category;

    /**
     * 关联用户
     */
    private Long userId;

    /**
     * $column.columnComment
     */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date createdAt;

    /**
     * $column.columnComment
     */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date updatedAt;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setExpenseDate(Date expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Date getExpenseDate() {
        return expenseDate;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    public void setPayerType(String payerType) {
        this.payerType = payerType;
    }

    public String getPayerType() {
        return payerType;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
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
                .append("itemName", getItemName())
                .append("amount", getAmount())
                .append("expenseDate", getExpenseDate())
                .append("stage", getStage())
                .append("payerType", getPayerType())
                .append("category", getCategory())
                .append("remark", getRemark())
                .append("userId", getUserId())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
