package com.make.finance.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 员工工资明细对象 salary_record
 *
 * @author erqi
 * @date 2025-06-14
 */
public class SalaryRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    @Excel(name = "用户ID")
    private Long userId;

    /**
     * 基本工资
     */
    @Excel(name = "基本工资")
    private BigDecimal baseSalary;

    /**
     * 岗位工资
     */
    @Excel(name = "岗位工资")
    private BigDecimal positionSalary;

    /**
     * 绩效工资
     */
    @Excel(name = "绩效工资")
    private BigDecimal performanceSalary;

    /**
     * 津贴
     */
    @Excel(name = "津贴")
    private BigDecimal allowance;

    /**
     * 通讯补贴
     */
    @Excel(name = "通讯补贴")
    private BigDecimal phoneSubsidy;

    /**
     * 交通补贴
     */
    @Excel(name = "交通补贴")
    private BigDecimal transportSubsidy;

    /**
     * 餐补
     */
    @Excel(name = "餐补")
    private BigDecimal mealSubsidy;

    /**
     * 证书津贴
     */
    @Excel(name = "证书津贴")
    private BigDecimal certificateAllowance;

    /**
     * 其他补助
     */
    @Excel(name = "其他补助")
    private BigDecimal otherSubsidy;

    /**
     * 电脑补助
     */
    @Excel(name = "电脑补助")
    private BigDecimal computerSubsidy;

    /**
     * 奖金
     */
    @Excel(name = "奖金")
    private BigDecimal bonus;

    /**
     * 其他
     */
    @Excel(name = "其他")
    private BigDecimal other;

    /**
     * 其他1
     */
    @Excel(name = "其他1")
    private BigDecimal other1;

    /**
     * 调整工资
     */
    @Excel(name = "调整工资")
    private BigDecimal adjustSalary;

    /**
     * 应付职工薪酬
     */
    @Excel(name = "应付职工薪酬")
    private BigDecimal payableSalary;

    /**
     * 事假工资
     */
    @Excel(name = "事假工资")
    private BigDecimal personalLeaveDeduction;

    /**
     * 病假工资
     */
    @Excel(name = "病假工资")
    private BigDecimal sickLeaveDeduction;

    /**
     * 缺勤扣款
     */
    @Excel(name = "缺勤扣款")
    private BigDecimal absenceDeduction;

    /**
     * 扣款
     */
    @Excel(name = "扣款")
    private BigDecimal deduction;

    /**
     * 扣款1
     */
    @Excel(name = "扣款1")
    private BigDecimal deduction1;

    /**
     * 社保个人
     */
    @Excel(name = "社保个人")
    private BigDecimal socialSecurity;

    /**
     * 社保个人补缴
     */
    @Excel(name = "社保个人补缴")
    private BigDecimal socialSecurityBackpay;

    /**
     * 公积金个人
     */
    @Excel(name = "公积金个人")
    private BigDecimal housingFund;

    /**
     * 公积金个人补缴
     */
    @Excel(name = "公积金个人补缴")
    private BigDecimal housingFundBackpay;

    /**
     * 税率
     */
    @Excel(name = "税率")
    private BigDecimal taxRate;

    /**
     * 个税
     */
    @Excel(name = "个税")
    private BigDecimal incomeTax;

    /**
     * 扣款合计
     */
    @Excel(name = "扣款合计")
    private BigDecimal totalDeduction;

    /**
     * 不付
     */
    @Excel(name = "不付")
    private BigDecimal unpaid;

    /**
     * 累计子女教育
     */
    @Excel(name = "累计子女教育")
    private BigDecimal totalChildEducation;

    /**
     * 累计赡养老人
     */
    @Excel(name = "累计赡养老人")
    private BigDecimal totalElderSupport;

    /**
     * 累计住房贷款
     */
    @Excel(name = "累计住房贷款")
    private BigDecimal totalMortgage;

    /**
     * 累计继续教育
     */
    @Excel(name = "累计继续教育")
    private BigDecimal totalEducation;

    /**
     * 累计住房租金
     */
    @Excel(name = "累计住房租金")
    private BigDecimal totalRent;

    /**
     * 累计婴幼儿照护费用
     */
    @Excel(name = "累计婴幼儿照护费用")
    private BigDecimal totalBabyCare;

    /**
     * 累计个人养老金
     */
    @Excel(name = "累计个人养老金")
    private BigDecimal totalPersonalPension;

    /**
     * 工资代发单位
     */
    @Excel(name = "工资代发单位")
    private String salaryPayOrg;

    /**
     * 实发金额
     */
    @Excel(name = "实发金额")
    private BigDecimal netSalary;

    /**
     * 发放日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发放日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date issueDate;

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

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setPositionSalary(BigDecimal positionSalary) {
        this.positionSalary = positionSalary;
    }

    public BigDecimal getPositionSalary() {
        return positionSalary;
    }

    public void setPerformanceSalary(BigDecimal performanceSalary) {
        this.performanceSalary = performanceSalary;
    }

    public BigDecimal getPerformanceSalary() {
        return performanceSalary;
    }

    public void setAllowance(BigDecimal allowance) {
        this.allowance = allowance;
    }

    public BigDecimal getAllowance() {
        return allowance;
    }

    public void setPhoneSubsidy(BigDecimal phoneSubsidy) {
        this.phoneSubsidy = phoneSubsidy;
    }

    public BigDecimal getPhoneSubsidy() {
        return phoneSubsidy;
    }

    public void setTransportSubsidy(BigDecimal transportSubsidy) {
        this.transportSubsidy = transportSubsidy;
    }

    public BigDecimal getTransportSubsidy() {
        return transportSubsidy;
    }

    public void setMealSubsidy(BigDecimal mealSubsidy) {
        this.mealSubsidy = mealSubsidy;
    }

    public BigDecimal getMealSubsidy() {
        return mealSubsidy;
    }

    public void setCertificateAllowance(BigDecimal certificateAllowance) {
        this.certificateAllowance = certificateAllowance;
    }

    public BigDecimal getCertificateAllowance() {
        return certificateAllowance;
    }

    public void setOtherSubsidy(BigDecimal otherSubsidy) {
        this.otherSubsidy = otherSubsidy;
    }

    public BigDecimal getOtherSubsidy() {
        return otherSubsidy;
    }

    public void setComputerSubsidy(BigDecimal computerSubsidy) {
        this.computerSubsidy = computerSubsidy;
    }

    public BigDecimal getComputerSubsidy() {
        return computerSubsidy;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setOther(BigDecimal other) {
        this.other = other;
    }

    public BigDecimal getOther() {
        return other;
    }

    public void setOther1(BigDecimal other1) {
        this.other1 = other1;
    }

    public BigDecimal getOther1() {
        return other1;
    }

    public void setAdjustSalary(BigDecimal adjustSalary) {
        this.adjustSalary = adjustSalary;
    }

    public BigDecimal getAdjustSalary() {
        return adjustSalary;
    }

    public void setPayableSalary(BigDecimal payableSalary) {
        this.payableSalary = payableSalary;
    }

    public BigDecimal getPayableSalary() {
        return payableSalary;
    }

    public void setPersonalLeaveDeduction(BigDecimal personalLeaveDeduction) {
        this.personalLeaveDeduction = personalLeaveDeduction;
    }

    public BigDecimal getPersonalLeaveDeduction() {
        return personalLeaveDeduction;
    }

    public void setSickLeaveDeduction(BigDecimal sickLeaveDeduction) {
        this.sickLeaveDeduction = sickLeaveDeduction;
    }

    public BigDecimal getSickLeaveDeduction() {
        return sickLeaveDeduction;
    }

    public void setAbsenceDeduction(BigDecimal absenceDeduction) {
        this.absenceDeduction = absenceDeduction;
    }

    public BigDecimal getAbsenceDeduction() {
        return absenceDeduction;
    }

    public void setDeduction(BigDecimal deduction) {
        this.deduction = deduction;
    }

    public BigDecimal getDeduction() {
        return deduction;
    }

    public void setDeduction1(BigDecimal deduction1) {
        this.deduction1 = deduction1;
    }

    public BigDecimal getDeduction1() {
        return deduction1;
    }

    public void setSocialSecurity(BigDecimal socialSecurity) {
        this.socialSecurity = socialSecurity;
    }

    public BigDecimal getSocialSecurity() {
        return socialSecurity;
    }

    public void setSocialSecurityBackpay(BigDecimal socialSecurityBackpay) {
        this.socialSecurityBackpay = socialSecurityBackpay;
    }

    public BigDecimal getSocialSecurityBackpay() {
        return socialSecurityBackpay;
    }

    public void setHousingFund(BigDecimal housingFund) {
        this.housingFund = housingFund;
    }

    public BigDecimal getHousingFund() {
        return housingFund;
    }

    public void setHousingFundBackpay(BigDecimal housingFundBackpay) {
        this.housingFundBackpay = housingFundBackpay;
    }

    public BigDecimal getHousingFundBackpay() {
        return housingFundBackpay;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setIncomeTax(BigDecimal incomeTax) {
        this.incomeTax = incomeTax;
    }

    public BigDecimal getIncomeTax() {
        return incomeTax;
    }

    public void setTotalDeduction(BigDecimal totalDeduction) {
        this.totalDeduction = totalDeduction;
    }

    public BigDecimal getTotalDeduction() {
        return totalDeduction;
    }

    public void setUnpaid(BigDecimal unpaid) {
        this.unpaid = unpaid;
    }

    public BigDecimal getUnpaid() {
        return unpaid;
    }

    public void setTotalChildEducation(BigDecimal totalChildEducation) {
        this.totalChildEducation = totalChildEducation;
    }

    public BigDecimal getTotalChildEducation() {
        return totalChildEducation;
    }

    public void setTotalElderSupport(BigDecimal totalElderSupport) {
        this.totalElderSupport = totalElderSupport;
    }

    public BigDecimal getTotalElderSupport() {
        return totalElderSupport;
    }

    public void setTotalMortgage(BigDecimal totalMortgage) {
        this.totalMortgage = totalMortgage;
    }

    public BigDecimal getTotalMortgage() {
        return totalMortgage;
    }

    public void setTotalEducation(BigDecimal totalEducation) {
        this.totalEducation = totalEducation;
    }

    public BigDecimal getTotalEducation() {
        return totalEducation;
    }

    public void setTotalRent(BigDecimal totalRent) {
        this.totalRent = totalRent;
    }

    public BigDecimal getTotalRent() {
        return totalRent;
    }

    public void setTotalBabyCare(BigDecimal totalBabyCare) {
        this.totalBabyCare = totalBabyCare;
    }

    public BigDecimal getTotalBabyCare() {
        return totalBabyCare;
    }

    public void setTotalPersonalPension(BigDecimal totalPersonalPension) {
        this.totalPersonalPension = totalPersonalPension;
    }

    public BigDecimal getTotalPersonalPension() {
        return totalPersonalPension;
    }

    public void setSalaryPayOrg(String salaryPayOrg) {
        this.salaryPayOrg = salaryPayOrg;
    }

    public String getSalaryPayOrg() {
        return salaryPayOrg;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getIssueDate() {
        return issueDate;
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
                .append("userId", getUserId())
                .append("baseSalary", getBaseSalary())
                .append("positionSalary", getPositionSalary())
                .append("performanceSalary", getPerformanceSalary())
                .append("allowance", getAllowance())
                .append("phoneSubsidy", getPhoneSubsidy())
                .append("transportSubsidy", getTransportSubsidy())
                .append("mealSubsidy", getMealSubsidy())
                .append("certificateAllowance", getCertificateAllowance())
                .append("otherSubsidy", getOtherSubsidy())
                .append("computerSubsidy", getComputerSubsidy())
                .append("bonus", getBonus())
                .append("other", getOther())
                .append("other1", getOther1())
                .append("adjustSalary", getAdjustSalary())
                .append("payableSalary", getPayableSalary())
                .append("personalLeaveDeduction", getPersonalLeaveDeduction())
                .append("sickLeaveDeduction", getSickLeaveDeduction())
                .append("absenceDeduction", getAbsenceDeduction())
                .append("deduction", getDeduction())
                .append("deduction1", getDeduction1())
                .append("socialSecurity", getSocialSecurity())
                .append("socialSecurityBackpay", getSocialSecurityBackpay())
                .append("housingFund", getHousingFund())
                .append("housingFundBackpay", getHousingFundBackpay())
                .append("taxRate", getTaxRate())
                .append("incomeTax", getIncomeTax())
                .append("totalDeduction", getTotalDeduction())
                .append("unpaid", getUnpaid())
                .append("totalChildEducation", getTotalChildEducation())
                .append("totalElderSupport", getTotalElderSupport())
                .append("totalMortgage", getTotalMortgage())
                .append("totalEducation", getTotalEducation())
                .append("totalRent", getTotalRent())
                .append("totalBabyCare", getTotalBabyCare())
                .append("totalPersonalPension", getTotalPersonalPension())
                .append("salaryPayOrg", getSalaryPayOrg())
                .append("netSalary", getNetSalary())
                .append("remark", getRemark())
                .append("issueDate", getIssueDate())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
