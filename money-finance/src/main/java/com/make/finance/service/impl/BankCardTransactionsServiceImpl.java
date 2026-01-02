package com.make.finance.service.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.BankCardTransactionsMapper;
import com.make.finance.domain.BankCardTransactions;
import com.make.finance.service.IBankCardTransactionsService;

/**
 * 银行流水Service业务层处理
 *
 * @author erqi
 * @date 2025-05-27
 */
@Service
public class BankCardTransactionsServiceImpl implements IBankCardTransactionsService {
    @Autowired
    private BankCardTransactionsMapper bankCardTransactionsMapper;

    /**
     * 查询银行流水
     *
     * @param id 银行流水主键
     * @return 银行流水
     */
    @Override
    public BankCardTransactions selectBankCardTransactionsById(Long id) {
        return bankCardTransactionsMapper.selectBankCardTransactionsById(id);
    }

    /**
     * 查询银行流水列表
     *
     * @param bankCardTransactions 银行流水
     * @return 银行流水
     */
    @Override
    public List<BankCardTransactions> selectBankCardTransactionsList(BankCardTransactions bankCardTransactions) {
        return bankCardTransactionsMapper.selectBankCardTransactionsList(bankCardTransactions);
    }

    /**
     * 新增银行流水
     *
     * @param bankCardTransactions 银行流水
     * @return 结果
     */
    @Override
    public int insertBankCardTransactions(BankCardTransactions bankCardTransactions) {
        return bankCardTransactionsMapper.insertBankCardTransactions(bankCardTransactions);
    }

    /**
     * 修改银行流水
     *
     * @param bankCardTransactions 银行流水
     * @return 结果
     */
    @Override
    public int updateBankCardTransactions(BankCardTransactions bankCardTransactions) {
        return bankCardTransactionsMapper.updateBankCardTransactions(bankCardTransactions);
    }

    /**
     * 批量删除银行流水
     *
     * @param ids 需要删除的银行流水主键
     * @return 结果
     */
    @Override
    public int deleteBankCardTransactionsByIds(Long[] ids) {
        return bankCardTransactionsMapper.deleteBankCardTransactionsByIds(ids);
    }

    /**
     * 删除银行流水信息
     *
     * @param id 银行流水主键
     * @return 结果
     */
    @Override
    public int deleteBankCardTransactionsById(Long id) {
        return bankCardTransactionsMapper.deleteBankCardTransactionsById(id);
    }

    /**
     * 查询银行卡流水解析列表
     *
     * @return 银行卡流水解析
     */
    @Override
    public List<BankCardTransactions> queryBankCardTransactionsYearList(Long id, String startDate, String endDate) {
        return bankCardTransactionsMapper.selectBankCardTransactionsYearList(id, startDate, endDate);
    }
    @Override
    public int batchInsertBankCardTransactions(List<BankCardTransactions> transactions) {
        if (CollectionUtils.isEmpty(transactions)) {
            return 0;
        }

        // 使用 MyBatis 的批量操作或分批处理以提高性能
        int total = 0;
        int batchSize = 1000; // 每批处理1000条记录

        for (int i = 0; i < transactions.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, transactions.size());
            List<BankCardTransactions> batch = transactions.subList(i, endIndex);

            // 调用批量插入的Mapper方法
            total += bankCardTransactionsMapper.batchInsertBankCardTransactions(batch);
        }

        return total;
    }


}
