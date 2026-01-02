package com.make.finance.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.make.common.utils.SecurityUtils;
import com.make.finance.domain.TransactionRecords;
import com.make.finance.mapper.TransactionRecordsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.ExpenseMapper;
import com.make.finance.domain.Expense;
import com.make.finance.service.IExpenseService;

/**
 * 消费Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class ExpenseServiceImpl implements IExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private TransactionRecordsMapper transactionRecordsMapper;

    /**
     * 查询消费
     *
     * @param expenseId 消费主键
     * @return 消费
     */
    @Override
    public Expense selectExpenseByExpenseId(Long expenseId) {
        return expenseMapper.selectExpenseByExpenseId(expenseId);
    }

    /**
     * 查询消费列表
     *
     * @param expense 消费
     * @return 消费
     */
    @Override
    public List<Expense> selectExpenseList(Expense expense) {
        return expenseMapper.selectExpenseList(expense);
    }

    /**
     * 查询消费月度统计
     *
     * @param expense 消费
     * @return 结果
     */
    @Override
    public List<Map<String, Object>> selectExpenseStats(Expense expense) {
        return expenseMapper.selectExpenseStats(expense);
    }

    /**
     * 新增消费
     *
     * @param expense 消费
     * @return 结果
     */
    @Override
    public int insertExpense(Expense expense) {
        return expenseMapper.insertExpense(expense);
    }

    /**
     * 修改消费
     *
     * @param expense 消费
     * @return 结果
     */
    @Override
    public int updateExpense(Expense expense) {
        return expenseMapper.updateExpense(expense);
    }

    /**
     * 批量删除消费
     *
     * @param expenseIds 需要删除的消费主键
     * @return 结果
     */
    @Override
    public int deleteExpenseByExpenseIds(Long[] expenseIds) {
        return expenseMapper.deleteExpenseByExpenseIds(expenseIds);
    }

    /**
     * 删除消费信息
     *
     * @param expenseId 消费主键
     * @return 结果
     */
    @Override
    public int deleteExpenseByExpenseId(Long expenseId) {
        return expenseMapper.deleteExpenseByExpenseId(expenseId);
    }

    @Override
    public BigDecimal getCurrentMonthExpenseTotal(Long id, LocalDate startDate, LocalDate endDate) {
        return expenseMapper.selectCurrentMonthExpenseTotal(id, startDate, endDate);
    }

    /**
     * 同步交易记录到支出表的工具方法。
     * <p>
     * 逻辑说明：
     * 1. 遍历所有交易记录（TransactionRecords）。
     * 2. 仅处理支付方式以“招商银行储蓄卡”开头的交易。
     * 3. 对于每笔交易：
     * - 若为正常交易且未重复、非退款类、不属于特定商户（如贝壳找房平台商户），则插入到支出表。
     * - 若为退款或特定商户交易，则删除对应支出记录。
     * <p>
     * 主要优化点：
     * 1. 减少重复条件判断，将退款和删除逻辑统一处理。
     * 2. 明确日志内容，帮助后续追踪。
     * 3. 增加详细注释以说明业务逻辑。
     */
    @Override
    public void syncExpense() {


        Long userId = SecurityUtils.getUserId();
        // =========================
        // 1. 查询所有交易
        // =========================
        TransactionRecords query = new TransactionRecords();
        query.setUserId(userId);
        List<TransactionRecords> records =
                transactionRecordsMapper.selectTransactionRecordsList(query);

        if (records == null || records.isEmpty()) {
            return;
        }

        // =========================
        // 2. Counterparty 排除关键字（可扩展）
        // =========================
        List<String> excludedCounterpartyKeywords = Arrays.asList(
                "贝壳",
                "梁月朋",
                "测试商户"
                // 后续可继续追加
        );

        // =========================
        // 3. 按月汇总金额
        // =========================
        Map<YearMonth, BigDecimal> monthAmountMap = new HashMap<>();

        for (TransactionRecords record : records) {

            // 仅招商银行储蓄卡
            if (record.getPaymentMethod() == null ||
                    !record.getPaymentMethod().startsWith("招商银行储蓄卡")) {
                continue;
            }

            // 排除 Counterparty 包含特定字符串
            String counterparty = record.getCounterparty();
            if (counterparty != null) {
                boolean excluded = excludedCounterpartyKeywords.stream()
                        .anyMatch(counterparty::contains);
                if (excluded) {
                    continue;
                }
            }

            // 必要字段校验
            if (record.getAmount() == null || record.getTransactionTime() == null) {
                continue;
            }

            // Date → LocalDateTime
            Date transactionTime = record.getTransactionTime();
            LocalDateTime localDateTime = transactionTime.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // LocalDateTime → YearMonth（如 2025-01）
            YearMonth yearMonth = YearMonth.from(localDateTime);

            // 是否退款
            boolean isRefund =
                    "退款".equals(record.getTransactionType()) ||
                            "退款".equals(record.getProductType()) ||
                            (record.getTransactionStatus() != null &&
                                    record.getTransactionStatus().contains("退款"));

            // 金额处理（退款负数）
            BigDecimal amount = record.getAmount().abs();
            if (isRefund) {
                amount = amount.negate();
            }

            // 汇总
            monthAmountMap.merge(yearMonth, amount, BigDecimal::add);
        }

        // =========================
        // 4. 按月写入 Expense
        // =========================
        for (Map.Entry<YearMonth, BigDecimal> entry : monthAmountMap.entrySet()) {

            BigDecimal totalAmount = entry.getValue();
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            YearMonth yearMonth = entry.getKey();

            // 该月第一天：2025-01-01
            LocalDate monthFirstDay = yearMonth.atDay(1);
            Date expenseDate = Date.from(
                    monthFirstDay.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );

            Expense queryExpense = new Expense();
            queryExpense.setUserId(userId);
            queryExpense.setExpenseDate(expenseDate);

            List<Expense> existList =
                    expenseMapper.selectExpenseList(queryExpense);

            if (existList != null && !existList.isEmpty()) {
                // 更新
                Expense exist = existList.get(0);
                exist.setAmount(totalAmount);
                expenseMapper.updateExpense(exist);
            } else {
                // 新增
                Expense expense = new Expense();
                expense.setUserId(userId);
                expense.setMerchant("微信/支付宝");
                expense.setCategory("微信/支付宝");
                expense.setAmount(totalAmount);
                expense.setExpenseDate(expenseDate);
                expenseMapper.insertExpense(expense);
            }
        }
    }


    @Override
    public Expense selectExpenseByUserIdAndLoan(long userId, Date repaymentDate) {
        return expenseMapper.selectExpenseByUserIdAndLoan(userId, repaymentDate);
    }

}
