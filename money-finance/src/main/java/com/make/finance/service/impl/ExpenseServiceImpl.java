package com.make.finance.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     *    - 若为正常交易且未重复、非退款类、不属于特定商户（如贝壳找房平台商户），则插入到支出表。
     *    - 若为退款或特定商户交易，则删除对应支出记录。
     * <p>
     * 主要优化点：
     * 1. 减少重复条件判断，将退款和删除逻辑统一处理。
     * 2. 明确日志内容，帮助后续追踪。
     * 3. 增加详细注释以说明业务逻辑。
     */
    @Override
    public void syncExpense() {
        // 获取所有交易记录
        List<TransactionRecords> transactionRecords = transactionRecordsMapper.selectTransactionRecordsList(new TransactionRecords());

        // 使用 Set 记录已处理的交易单号，避免重复操作
        Set<String> processedTransactionIds = new HashSet<>();

        for (TransactionRecords record : transactionRecords) {
            // 仅处理招商银行储蓄卡交易
            String paymentMethod = record.getPaymentMethod();
            if (paymentMethod == null || !paymentMethod.startsWith("招商银行储蓄卡")) {
                continue;
            }

            String transactionId = record.getTransactionId();
            String transactionType = record.getTransactionType();
            String productType = record.getProductType();
            String counterparty = record.getCounterparty();
            String transactionStatus = record.getTransactionStatus();

            // 构建支出查询条件（根据交易单号和用户 ID）
            Expense expenseQuery = new Expense();
            expenseQuery.setMerchant(transactionId);
            expenseQuery.setUserId(record.getUserId());

            List<Expense> matchedExpenses = expenseMapper.selectExpenseList(expenseQuery);

            // 条件1：非退款类、未重复、未存在记录、非特定商户
            boolean isNormalTransaction =
                    !"退款".equals(transactionType) &&
                            !"退款".equals(productType) &&
                            (counterparty == null || !counterparty.equals("贝壳找房平台商户")) &&
                            (transactionStatus == null || !transactionStatus.contains("退款")) &&
                            !processedTransactionIds.contains(transactionId) &&
                            matchedExpenses.isEmpty();

            if (isNormalTransaction) {
                // 插入支出记录
                Expense newExpense = new Expense();
                newExpense.setUserId(1L); // 默认用户 ID
                newExpense.setMerchant(transactionId);
                newExpense.setAmount(record.getAmount());
                newExpense.setCategory(productType);
                newExpense.setExpenseDate(record.getTransactionTime());

                expenseMapper.insertExpense(newExpense);
                processedTransactionIds.add(transactionId);

            } else {
                // 条件2：退款类或特定商户交易，执行删除逻辑
                boolean isRefundOrInvalid =
                        "退款".equals(transactionType) ||
                                "退款".equals(productType) ||
                                (counterparty != null && counterparty.equals("贝壳找房平台商户")) ||
                                (transactionStatus != null && transactionStatus.contains("退款"));

                if (isRefundOrInvalid) {
                    List<Expense> toDeleteExpenses = expenseMapper.selectExpenseListByTransactionId(expenseQuery);
                    for (Expense e : toDeleteExpenses) {
                        processedTransactionIds.add(e.getMerchant());
                        expenseMapper.deleteExpenseByExpenseId(e.getExpenseId());
                    }

                    // 根据不同原因输出日志
                    if ("退款".equals(transactionType) || "退款".equals(productType)) {
                        log.info("退款交易，删除的交易单号: {}", transactionId);
                    } else if (counterparty != null && counterparty.equals("贝壳找房平台商户")) {
                        log.info("特定商户(贝壳找房平台商户)交易，删除的交易单号: {}", transactionId);
                    } else if (transactionStatus != null && transactionStatus.contains("退款")) {
                        log.info("交易状态为退款，删除的交易单号: {}", transactionId);
                    }
                }
            }
        }
    }

    @Override
    public Expense selectExpenseByUserIdAndLoan(long userId, Date repaymentDate) {
        return expenseMapper.selectExpenseByUserIdAndLoan(userId, repaymentDate);
    }

}
