package com.make.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import com.make.finance.domain.vo.LabelEntity;
import com.make.finance.domain.Income;

/**
 * 收入Service接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface IIncomeService {
    /**
     * 查询收入
     *
     * @param incomeId 收入主键
     * @return 收入
     */
    public Income selectIncomeByIncomeId(Long incomeId);

    /**
     * 查询收入列表
     *
     * @param income 收入
     * @return 收入集合
     */
    public List<Income> selectIncomeList(Income income);

    /**
     * 新增收入
     *
     * @param income 收入
     * @return 结果
     */
    public int insertIncome(Income income);

    /**
     * 修改收入
     *
     * @param income 收入
     * @return 结果
     */
    public int updateIncome(Income income);

    /**
     * 批量删除收入
     *
     * @param incomeIds 需要删除的收入主键集合
     * @return 结果
     */
    public int deleteIncomeByIncomeIds(Long[] incomeIds);

    /**
     * 删除收入信息
     *
     * @param incomeId 收入主键
     * @return 结果
     */
    public int deleteIncomeByIncomeId(Long incomeId);

    /**
     * 获取收入来源
     *
     * @return 结果
     */
    List<LabelEntity> getSourceOptions();

    /**
     * 获取当月收入总和
     *
     * @param startDate
     * @param endDate
     * @return 结果
     */
    BigDecimal getCurrentMonthIncomeTotal(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 仅查询用户工资收入
     *
     * @param userId 用户ID
     * @return 收入
     */
    public Income getIncomeByUserIdAndDate(Long userId, Date date);
}
