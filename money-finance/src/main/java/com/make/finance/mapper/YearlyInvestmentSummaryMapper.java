package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.YearlyInvestmentSummary;

/**
 * 年度投资汇总Mapper接口
 *
 * @author erqi
 * @date 2025-07-07
 */
public interface YearlyInvestmentSummaryMapper {

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
     * 删除年度投资汇总
     *
     * @param id 年度投资汇总主键
     * @return 结果
     */
    public int deleteYearlyInvestmentSummaryById(Long id);

    /**
     * 批量删除年度投资汇总
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYearlyInvestmentSummaryByIds(Long[] ids);
}
