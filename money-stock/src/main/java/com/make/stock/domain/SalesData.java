package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 利润折线图数据对象 sales_data
 *
 * @author erqi
 * @date 2025-07-29
 */
public class SalesData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增
     */
    private Long id;

    /**
     * 数据记录日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "数据记录日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date recordDate;

    /**
     * 当天利润（万元）
     */
    @Excel(name = "当天利润", readConverterExp = "万=元")
    private BigDecimal profit;

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

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }

    public Date getRecordDate() {
        return recordDate;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("recordDate", getRecordDate())
                .append("profit", getProfit())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("userId", getUserId())
                .toString();
    }
}
