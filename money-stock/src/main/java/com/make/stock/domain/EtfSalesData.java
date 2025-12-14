package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eft折线图数据对象 etf_sales_data
 *
 * @author erqi
 * @date 2025-05-28
 */
public class EtfSalesData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增
     */
    private Long id;

    /**
     * 数据记录日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "数据记录日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date recordDate;

    /**
     * ETF代码
     */
    @Excel(name = "ETF代码")
    private String etfCode;

    /**
     * ETF名称
     */
    @Excel(name = "ETF名称")
    private String etfName;

    /**
     * 分时利润（元）
     */
    @Excel(name = "分时利润", readConverterExp = "元=")
    private BigDecimal profit;

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

    public void setEtfCode(String etfCode) {
        this.etfCode = etfCode;
    }

    public String getEtfCode() {
        return etfCode;
    }

    public void setEtfName(String etfName) {
        this.etfName = etfName;
    }

    public String getEtfName() {
        return etfName;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("recordDate", getRecordDate())
                .append("etfCode", getEtfCode())
                .append("etfName", getEtfName())
                .append("profit", getProfit())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
