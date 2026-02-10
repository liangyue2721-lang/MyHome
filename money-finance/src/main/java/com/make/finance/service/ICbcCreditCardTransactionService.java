package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.CbcCreditCardTransaction;

/**
 * 建行信用卡交易记录Service接口
 *
 * @author 贰柒
 * @date 2025-05-26
 */
public interface ICbcCreditCardTransactionService {
    /**
     * 查询建行信用卡交易记录
     *
     * @param id 建行信用卡交易记录主键
     * @return 建行信用卡交易记录
     */
    public CbcCreditCardTransaction selectCbcCreditCardTransactionById(Long id);

    /**
     * 查询建行信用卡交易记录列表
     *
     * @param cbcCreditCardTransaction 建行信用卡交易记录
     * @return 建行信用卡交易记录集合
     */
    public List<CbcCreditCardTransaction> selectCbcCreditCardTransactionList(CbcCreditCardTransaction cbcCreditCardTransaction);

    /**
     * 新增建行信用卡交易记录
     *
     * @param cbcCreditCardTransaction 建行信用卡交易记录
     * @return 结果
     */
    public int insertCbcCreditCardTransaction(CbcCreditCardTransaction cbcCreditCardTransaction);

    /**
     * 修改建行信用卡交易记录
     *
     * @param cbcCreditCardTransaction 建行信用卡交易记录
     * @return 结果
     */
    public int updateCbcCreditCardTransaction(CbcCreditCardTransaction cbcCreditCardTransaction);

    /**
     * 批量删除建行信用卡交易记录
     *
     * @param ids 需要删除的建行信用卡交易记录主键集合
     * @return 结果
     */
    public int deleteCbcCreditCardTransactionByIds(Long[] ids);

    /**
     * 删除建行信用卡交易记录信息
     *
     * @param id 建行信用卡交易记录主键
     * @return 结果
     */
    public int deleteCbcCreditCardTransactionById(Long id);

    /**
     * 批量新增建行信用卡交易记录
     *
     * @param cbcCreditCardTransactions 建行信用卡交易记录列表
     * @return 插入成功的记录数
     */
    public int batchInsertCbcCreditCardTransaction(List<CbcCreditCardTransaction> cbcCreditCardTransactions);
}
