package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 个人资产明细对象 asset_record
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public class AssetRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 资产记录ID
     */
    private Long assetId;

    /**
     * 用户ID
     */
    @Excel(name = "用户ID")
    private Long userId;

    /**
     * 资产类型[1流动资产 2投资资产 3固定资产]
     */
    @Excel(name = "资产类型[1流动资产 2投资资产 3固定资产]")
    private Integer assetType;

    /**
     * 资产价值
     */
    @Excel(name = "资产价值")
    private BigDecimal amount;

    /**
     * 货币类型(ISO 4217)
     */
    @Excel(name = "货币类型(ISO 4217)")
    private String currency;

    /**
     * 状态[1有效 0冻结 9已清算]
     */
    @Excel(name = "状态[1有效 0冻结 9已清算]")
    private Integer assetStatus;

    /**
     * 来源渠道[bank/stock/fund/real_estate...]
     */
    @Excel(name = "来源渠道[bank/stock/fund/real_estate...]")
    private String sourceChannel;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setAssetType(Integer assetType) {
        this.assetType = assetType;
    }

    public Integer getAssetType() {
        return assetType;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setAssetStatus(Integer assetStatus) {
        this.assetStatus = assetStatus;
    }

    public Integer getAssetStatus() {
        return assetStatus;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getSourceChannel() {
        return sourceChannel;
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
                .append("assetId", getAssetId())
                .append("userId", getUserId())
                .append("assetType", getAssetType())
                .append("amount", getAmount())
                .append("currency", getCurrency())
                .append("assetStatus", getAssetStatus())
                .append("sourceChannel", getSourceChannel())
                .append("remark", getRemark())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
