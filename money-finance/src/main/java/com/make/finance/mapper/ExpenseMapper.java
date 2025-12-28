package com.make.finance.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.make.finance.domain.Expense;
import org.apache.ibatis.annotations.Param;

/**
 * 消费Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface ExpenseMapper {
    /**
     * 查询消费
     *
     * @param expenseId 消费主键
     * @return 消费
     */
    public Expense selectExpenseByExpenseId(Long expenseId);

    /**
     * 查询消费列表
     *
     * @param expense 消费
     * @return 消费集合
     */
    public List<Expense> selectExpenseList(Expense expense);

    /**
     * 新增消费
     *
     * @param expense 消费
     * @return 结果
     */
    public int insertExpense(Expense expense);

    /**
     * 修改消费
     *
     * @param expense 消费
     * @return 结果
     */
    public int updateExpense(Expense expense);

    /**
     * 删除消费
     *
     * @param expenseId 消费主键
     * @return 结果
     */
    public int deleteExpenseByExpenseId(Long expenseId);

    /**
     * 批量删除消费
     *
     * @param expenseIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteExpenseByExpenseIds(Long[] expenseIds);

    /**
     * 查询指定时间范围内的总支出金额
     *
     * <p>该方法通过时间范围统计所有支出记录的累计金额，结果精确到分（两位小数）</p>
     *
     * @param id
     * @param startDate 统计起始日期（包含当日），格式为{@code yyyy-MM-dd}
     * @param endDate   统计结束日期（包含当日），需满足{@code endDate >= startDate}
     * @return 当前时间段内的总支出金额，格式为{@code 12345.67}（单位：元
     * @throws IllegalArgumentException 当{@code startDate}晚于{@code endDate}时抛出
     * @apiNote 建议使用{@linkplain LocalDate#atStartOfDay() 时间范围边界处理}
     * @example <pre>{@code
     * BigDecimal total = service.selectCurrentMonthExpenseTotal(
     *     LocalDate.of(2025, 5, 1),
     *     LocalDate.of(2025, 5, 31)
     * );
     * // 输出：BigDecimal[12500.75](@ref)}</pre>
     */
    BigDecimal selectCurrentMonthExpenseTotal(@Param("userId") Long userId, @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 查询消费月度统计
     *
     * @param expense 消费
     * @return 结果
     */
    public List<Map<String, Object>> selectExpenseStats(Expense expense);

    /**
     * 查询异常消费
     *
     * @param expense 消费
     * @return 消费集合
     */
    public List<Expense> selectExpenseListByTransactionId(Expense expense);

    Expense selectExpenseByUserIdAndLoan(@Param("userId") long userId, @Param("expenseDate") Date expenseDate);
}
