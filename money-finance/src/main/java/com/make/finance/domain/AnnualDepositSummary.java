package com.make.finance.domain;

import java.math.BigDecimal;

import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 年度存款统计对象 annual_deposit_summary
 *
 * @author erqi
 * @date 2025-07-20
 */
public class AnnualDepositSummary extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 统计年份（如2025）
     */
    @Excel(name = "统计年份", readConverterExp = "如=2025")
    private Long year;

    /**
     * 年度存款总额（单位：元）
     */
    @Excel(name = "年度存款总额", readConverterExp = "单=位：元")
    private BigDecimal totalDeposit;

    /**
     * 用户 ID，关联用户表
     */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setYear(Long year) {
        this.year = year;
    }

    public Long getYear() {
        return year;
    }

    public void setTotalDeposit(BigDecimal totalDeposit) {
        this.totalDeposit = totalDeposit;
    }

    public BigDecimal getTotalDeposit() {
        return totalDeposit;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("year", getYear())
                .append("totalDeposit", getTotalDeposit())
                .append("remark", getRemark())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
