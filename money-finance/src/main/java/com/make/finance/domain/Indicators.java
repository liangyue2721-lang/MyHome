package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 指标信息对象 indicators
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public class Indicators extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识
     */
    private Long id;

    /**
     * 指标名称
     */
    @Excel(name = "指标名称")
    private String name;

    /**
     * 指标描述
     */
    @Excel(name = "指标描述")
    private String description;

    /**
     * 目标数值
     */
    @Excel(name = "目标数值")
    private BigDecimal targetValue;

    /**
     * 当前进度值
     */
    @Excel(name = "当前进度值")
    private BigDecimal currentValue;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /**
     * 计划完成日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划完成日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /**
     * 指标状态（active进行中、completed已完成、paused暂停、archived已归档）
     */
    @Excel(name = "指标状态", readConverterExp = "a=ctive进行中、completed已完成、paused暂停、archived已归档")
    private String status;

    /**
     * 操作人（如用户名或用户ID）
     */
    @Excel(name = "操作人", readConverterExp = "如=用户名或用户ID")
    private Long changedBy;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
    /**
     * 用户 ID，关联用户表
     */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public Long getChangedBy() {
        return changedBy;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("name", getName())
                .append("description", getDescription())
                .append("targetValue", getTargetValue())
                .append("currentValue", getCurrentValue())
                .append("startDate", getStartDate())
                .append("endDate", getEndDate())
                .append("status", getStatus())
                .append("changedBy", getChangedBy())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("userId", getUserId())
                .toString();
    }
}
