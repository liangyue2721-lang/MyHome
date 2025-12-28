package com.make.finance.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.make.finance.domain.Income;
import org.apache.ibatis.annotations.Param;

/**
 * 收入Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface IncomeMapper {
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
     * 删除收入
     *
     * @param incomeId 收入主键
     * @return 结果
     */
    public int deleteIncomeByIncomeId(Long incomeId);

    /**
     * 批量删除收入
     *
     * @param incomeIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIncomeByIncomeIds(Long[] incomeIds);

    BigDecimal selectCurrentMonthIncomeTotal(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Income selectIncomeByUserIdAndDate(@Param("userId") Long userId,@Param("incomeDate") Date incomeDate);

    List<Map<String, Object>> selectIncomeStats(Income income);
}
