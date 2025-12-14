package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.CbcCreditCardTransaction;

/**
 * 建行信用卡交易记录Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-26
 */
public interface CbcCreditCardTransactionMapper {
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
     * 删除建行信用卡交易记录
     *
     * @param id 建行信用卡交易记录主键
     * @return 结果
     */
    public int deleteCbcCreditCardTransactionById(Long id);

    /**
     * 批量删除建行信用卡交易记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCbcCreditCardTransactionByIds(Long[] ids);
}
