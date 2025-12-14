package com.make.stock.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 任务允许IP对象 task_allowed_ips
 *
 * @author erqi
 * @date 2025-06-19
 */
public class TaskAllowedIps extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private String id;

    /**
     * 允许的IP地址（二进制格式）
     */
    @Excel(name = "允许的IP地址", readConverterExp = "二=进制格式")
    private String allowedIp;

    /**
     * CIDR子网掩码（IPv4默认32，IPv6默认128）
     */
    @Excel(name = "CIDR子网掩码", readConverterExp = "I=Pv4默认32，IPv6默认128")
    private String cidrMask;

    /**
     * IP白名单描述
     */
    @Excel(name = "IP白名单描述")
    private String description;

    /**
     * 记录状态（是否启用）
     */
    @Excel(name = "记录状态", readConverterExp = "是=否启用")
    private String status;

    /**
     * 是否使用（1=使用中，0=未使用）
     */
    @Excel(name = "是否使用", readConverterExp = "1==使用中，0=未使用")
    private Integer isActive;

    /**
     * 记录当前生命周期状态
     */
    @Excel(name = "记录当前生命周期状态")
    private String state;

    /**
     * 优先级（数值越小优先级越高）
     */
    @Excel(name = "优先级", readConverterExp = "数=值越小优先级越高")
    private String priority;

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

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setAllowedIp(String allowedIp) {
        this.allowedIp = allowedIp;
    }

    public String getAllowedIp() {
        return allowedIp;
    }

    public void setCidrMask(String cidrMask) {
        this.cidrMask = cidrMask;
    }

    public String getCidrMask() {
        return cidrMask;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
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
                .append("allowedIp", getAllowedIp())
                .append("cidrMask", getCidrMask())
                .append("description", getDescription())
                .append("status", getStatus())
                .append("isActive", getIsActive())
                .append("state", getState())
                .append("priority", getPriority())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
