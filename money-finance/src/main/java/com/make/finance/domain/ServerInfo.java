package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 服务器有效期管理（MySQL5.7兼容版）对象 server_info
 *
 * @author erqi
 * @date 2025-10-15
 */
public class ServerInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 服务器名称
     */
    @Excel(name = "服务器名称")
    private String serverName;

    /**
     * 服务器IP地址
     */
    @Excel(name = "服务器IP地址")
    private String ipAddress;

    /**
     * 服务提供商（如阿里云、腾讯云、AWS）
     */
    @Excel(name = "服务提供商", readConverterExp = "如=阿里云、腾讯云、AWS")
    private String provider;

    /**
     * 操作系统类型（如CentOS 7、Ubuntu 20.04）
     */
    @Excel(name = "操作系统类型", readConverterExp = "如=CentOS,7=、Ubuntu,2=0.04")
    private String osType;

    /**
     * 购买日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "购买日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date purchaseDate;

    /**
     * 到期日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "到期日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireDate;

    /**
     * 到期前提醒天数（单位：天）
     */
    @Excel(name = "到期前提醒天数", readConverterExp = "单=位：天")
    private Long remindDays;

    /**
     * 购买或续费价格（元）
     */
    @Excel(name = "购买或续费价格", readConverterExp = "购买或续费价格（元）")
    private BigDecimal price;

    /**
     * 状态（active=使用中, expiring=即将到期, expired=已过期, stopped=停用）
     */
    @Excel(name = "状态", readConverterExp = "active=使用中,,expiring=即将到期,,expired=已过期,,stopped=停用")
    private String status;

    /**
     * 负责人姓名
     */
    @Excel(name = "负责人姓名")
    private String adminUser;

    /**
     * 负责人邮箱
     */
    @Excel(name = "负责人邮箱")
    private String contactEmail;

    /**
     * 是否已发送到期提醒（0=否,1=是）
     */
    @Excel(name = "是否已发送到期提醒", readConverterExp = "0==否,1=是")
    private Integer notifySent;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getOsType() {
        return osType;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setRemindDays(Long remindDays) {
        this.remindDays = remindDays;
    }

    public Long getRemindDays() {
        return remindDays;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setNotifySent(Integer notifySent) {
        this.notifySent = notifySent;
    }

    public Integer getNotifySent() {
        return notifySent;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("serverName", getServerName())
                .append("ipAddress", getIpAddress())
                .append("provider", getProvider())
                .append("osType", getOsType())
                .append("purchaseDate", getPurchaseDate())
                .append("expireDate", getExpireDate())
                .append("remindDays", getRemindDays())
                .append("price", getPrice())
                .append("status", getStatus())
                .append("adminUser", getAdminUser())
                .append("contactEmail", getContactEmail())
                .append("notifySent", getNotifySent())
                .append("remark", getRemark())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
