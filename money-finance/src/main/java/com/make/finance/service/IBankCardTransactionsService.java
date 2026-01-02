package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.BankCardTransactions;

/**
 * 银行流水Service接口
 *
 * @author erqi
 * @date 2025-05-27
 */
public interface IBankCardTransactionsService {
    /**
     * 查询银行流水
     *
     * @param id 银行流水主键
     * @return 银行流水
     */
    public BankCardTransactions selectBankCardTransactionsById(Long id);

    /**
     * 查询银行流水列表
     *
     * @param bankCardTransactions 银行流水
     * @return 银行流水集合
     */
    public List<BankCardTransactions> selectBankCardTransactionsList(BankCardTransactions bankCardTransactions);

    /**
     * 新增银行流水
     *
     * @param bankCardTransactions 银行流水
     * @return 结果
     */
    public int insertBankCardTransactions(BankCardTransactions bankCardTransactions);

    /**
     * 修改银行流水
     *
     * @param bankCardTransactions 银行流水
     * @return 结果
     */
    public int updateBankCardTransactions(BankCardTransactions bankCardTransactions);

    /**
     * 批量删除银行流水
     *
     * @param ids 需要删除的银行流水主键集合
     * @return 结果
     */
    public int deleteBankCardTransactionsByIds(Long[] ids);

    /**
     * 删除银行流水信息
     *
     * @param id 银行流水主键
     * @return 结果
     */
    public int deleteBankCardTransactionsById(Long id);

    /**
     * 查询银行卡流水解析
     *
     * @return 银行卡流水解析
     */
    public List<BankCardTransactions> queryBankCardTransactionsYearList(Long id, String startDate, String endDate);

    /**
     * 批量插入银行流水交易记录
     *
     * @param transactions 流水记录列表
     * @return 插入成功的记录数
     */
    int batchInsertBankCardTransactions(List<BankCardTransactions> transactions);

}
