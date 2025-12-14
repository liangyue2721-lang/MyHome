package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.YearlyInvestmentSummary;

/**
 * 年度投资汇总Service接口
 *
 * @author erqi
 * @date 2025-07-07
 */
public interface IYearlyInvestmentSummaryService {

    /**
     * 查询年度投资汇总
     *
     * @param id 年度投资汇总主键
     * @return 年度投资汇总
     */
    public YearlyInvestmentSummary selectYearlyInvestmentSummaryById(Long id);

    /**
     * 查询年度投资汇总列表
     *
     * @param yearlyInvestmentSummary 年度投资汇总
     * @return 年度投资汇总集合
     */
    public List<YearlyInvestmentSummary> selectYearlyInvestmentSummaryList(YearlyInvestmentSummary yearlyInvestmentSummary);

    /**
     * 新增年度投资汇总
     *
     * @param yearlyInvestmentSummary 年度投资汇总
     * @return 结果
     */
    public int insertYearlyInvestmentSummary(YearlyInvestmentSummary yearlyInvestmentSummary);

    /**
     * 修改年度投资汇总
     *
     * @param yearlyInvestmentSummary 年度投资汇总
     * @return 结果
     */
    public int updateYearlyInvestmentSummary(YearlyInvestmentSummary yearlyInvestmentSummary);

    /**
     * 批量删除年度投资汇总
     *
     * @param ids 需要删除的年度投资汇总主键集合
     * @return 结果
     */
    public int deleteYearlyInvestmentSummaryByIds(Long[] ids);

    /**
     * 删除年度投资汇总信息
     *
     * @param id 年度投资汇总主键
     * @return 结果
     */
    public int deleteYearlyInvestmentSummaryById(Long id);
}
