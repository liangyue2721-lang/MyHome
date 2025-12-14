package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 证券上市通知对象 stock_listing_notice
 *
 * @author erqi
 * @date 2025-07-31
 */
public class StockListingNotice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 证券代码
     */
    @Excel(name = "证券代码")
    private String securityCode;

    /**
     * 证券名称
     */
    @Excel(name = "证券名称")
    private String securityName;

    /**
     * 发行价格
     */
    @Excel(name = "发行价格")
    private BigDecimal issuePrice;

    /**
     * 上市日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "上市日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date listingDate;

    /**
     * 当前价格
     */
    @Excel(name = "当前价格")
    private BigDecimal currentPrice;

    /**
     * 每股净利润
     */
    @Excel(name = "每股净利润")
    private BigDecimal netProfit;

    /**
     * 利润率（如0.1234表示12.34%）
     */
    @Excel(name = "利润率", readConverterExp = "如=0.1234表示12.34%")
    private BigDecimal profitMargin;

    /**
     * 已通知次数
     */
    @Excel(name = "已通知次数")
    private Integer notifyCount;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setSecurityCode(String securityCode) {
        this.securityCode = securityCode;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setIssuePrice(BigDecimal issuePrice) {
        this.issuePrice = issuePrice;
    }

    public BigDecimal getIssuePrice() {
        return issuePrice;
    }

    public void setListingDate(Date listingDate) {
        this.listingDate = listingDate;
    }

    public Date getListingDate() {
        return listingDate;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin;
    }

    public void setNotifyCount(Integer notifyCount) {
        this.notifyCount = notifyCount;
    }

    public Integer getNotifyCount() {
        return notifyCount;
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
                .append("securityCode", getSecurityCode())
                .append("securityName", getSecurityName())
                .append("issuePrice", getIssuePrice())
                .append("listingDate", getListingDate())
                .append("currentPrice", getCurrentPrice())
                .append("netProfit", getNetProfit())
                .append("profitMargin", getProfitMargin())
                .append("notifyCount", getNotifyCount())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
