package com.make.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.YearlyInvestmentSummaryMapper;
import com.make.finance.domain.YearlyInvestmentSummary;
import com.make.finance.service.IYearlyInvestmentSummaryService;

/**
 * 年度投资汇总Service业务层处理
 *
 * @author erqi
 * @date 2025-07-07
 */
@Service
public class YearlyInvestmentSummaryServiceImpl implements IYearlyInvestmentSummaryService {

    @Autowired
    private YearlyInvestmentSummaryMapper yearlyInvestmentSummaryMapper;

    /**
     * 查询年度投资汇总
     *
     * @param id 年度投资汇总主键
     * @return 年度投资汇总
     */
    @Override
    public YearlyInvestmentSummary selectYearlyInvestmentSummaryById(Long id) {
        return yearlyInvestmentSummaryMapper.selectYearlyInvestmentSummaryById(id);
    }

    /**
     * 查询年度投资汇总列表
     *
     * @param yearlyInvestmentSummary 年度投资汇总
     * @return 年度投资汇总
     */
    @Override
    public List<YearlyInvestmentSummary> selectYearlyInvestmentSummaryList(YearlyInvestmentSummary yearlyInvestmentSummary) {
        return yearlyInvestmentSummaryMapper.selectYearlyInvestmentSummaryList(yearlyInvestmentSummary);
    }

    /**
     * 新增年度投资汇总
     *
     * @param yearlyInvestmentSummary 年度投资汇总
     * @return 结果
     */
    @Override
    public int insertYearlyInvestmentSummary(YearlyInvestmentSummary yearlyInvestmentSummary) {
        yearlyInvestmentSummary.setCreateTime(DateUtils.getNowDate());

        yearlyInvestmentSummary.setExpectedGrowthRate(calculateProfitRate(yearlyInvestmentSummary.getExpectedEndValue(),
                yearlyInvestmentSummary.getStartPrincipal()));
        yearlyInvestmentSummary.setActualGrowthRate(calculateProfitRate(yearlyInvestmentSummary.getActualEndValue(),
                yearlyInvestmentSummary.getStartPrincipal()));

        return yearlyInvestmentSummaryMapper.insertYearlyInvestmentSummary(yearlyInvestmentSummary);
    }

    /**
     * 修改年度投资汇总
     *
     * @param yearlyInvestmentSummary 年度投资汇总
     * @return 结果
     */
    @Override
    public int updateYearlyInvestmentSummary(YearlyInvestmentSummary yearlyInvestmentSummary) {
        yearlyInvestmentSummary.setUpdateTime(DateUtils.getNowDate());

        yearlyInvestmentSummary.setExpectedGrowthRate(calculateProfitRate(yearlyInvestmentSummary.getExpectedEndValue(),
                yearlyInvestmentSummary.getStartPrincipal()));
        yearlyInvestmentSummary.setActualGrowthRate(calculateProfitRate(yearlyInvestmentSummary.getActualEndValue(),
                yearlyInvestmentSummary.getStartPrincipal()));

        return yearlyInvestmentSummaryMapper.updateYearlyInvestmentSummary(yearlyInvestmentSummary);
    }

    /**
     * 批量删除年度投资汇总
     *
     * @param ids 需要删除的年度投资汇总主键
     * @return 结果
     */
    @Override
    public int deleteYearlyInvestmentSummaryByIds(Long[] ids) {
        return yearlyInvestmentSummaryMapper.deleteYearlyInvestmentSummaryByIds(ids);
    }

    /**
     * 删除年度投资汇总信息
     *
     * @param id 年度投资汇总主键
     * @return 结果
     */
    @Override
    public int deleteYearlyInvestmentSummaryById(Long id) {
        return yearlyInvestmentSummaryMapper.deleteYearlyInvestmentSummaryById(id);
    }


    /**
     * 计算收益率（针对本期投入）
     *
     * @param totalAsset      当前总资产（含收益）
     * @param principalAmount 本期投入本金
     * @return 当前收益率（百分比），保留两位小数；本金为0时返回0
     */
    public static BigDecimal calculateProfitRate(BigDecimal totalAsset, BigDecimal principalAmount) {
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (totalAsset == null) {
            totalAsset = BigDecimal.ZERO;
        }

        return totalAsset.subtract(principalAmount)
                .divide(principalAmount, 6, RoundingMode.HALF_UP) // 保留中间计算精度
                .multiply(new BigDecimal("100")) // 转为百分比
                .setScale(2, RoundingMode.HALF_UP); // 保留两位小数
    }
}
