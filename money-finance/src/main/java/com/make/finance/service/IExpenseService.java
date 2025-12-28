package com.make.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.make.finance.domain.Expense;

/**
 * 消费Service接口
 * 
 * @author 贰柒
 * @date 2025-05-28
 */
public interface IExpenseService 
{
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
     * 查询消费月度统计
     *
     * @param expense 消费
     * @return 结果
     */
    public List<Map<String, Object>> selectExpenseStats(Expense expense);

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
     * 批量删除消费
     * 
     * @param expenseIds 需要删除的消费主键集合
     * @return 结果
     */
    public int deleteExpenseByExpenseIds(Long[] expenseIds);

    /**
     * 删除消费信息
     * 
     * @param expenseId 消费主键
     * @return 结果
     */
    public int deleteExpenseByExpenseId(Long expenseId);

    /**
     * 获取当月消费总和
     *
     * @return 结果
     * @param startDate
     * @param endDate
     */
    BigDecimal getCurrentMonthExpenseTotal(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 同步现金流消费情况
     */
    public void syncExpense();


    Expense selectExpenseByUserIdAndLoan(long l, Date repaymentDate);

}
