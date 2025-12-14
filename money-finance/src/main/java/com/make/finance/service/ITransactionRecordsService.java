package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.TransactionRecords;

/**
 * 微信支付宝流水Service接口
 *
 * @author è´°æ
 * @date 2025-05-27
 */
public interface ITransactionRecordsService {
    /**
     * 查询微信支付宝流水
     *
     * @param id 微信支付宝流水主键
     * @return 微信支付宝流水
     */
    public TransactionRecords selectTransactionRecordsById(Long id);

    /**
     * 查询微信支付宝流水列表
     *
     * @param transactionRecords 微信支付宝流水
     * @return 微信支付宝流水集合
     */
    public List<TransactionRecords> selectTransactionRecordsList(TransactionRecords transactionRecords);

    /**
     * 新增微信支付宝流水
     *
     * @param transactionRecords 微信支付宝流水
     * @return 结果
     */
    public int insertTransactionRecords(TransactionRecords transactionRecords);

    /**
     * 修改微信支付宝流水
     *
     * @param transactionRecords 微信支付宝流水
     * @return 结果
     */
    public int updateTransactionRecords(TransactionRecords transactionRecords);

    /**
     * 批量删除微信支付宝流水
     *
     * @param ids 需要删除的微信支付宝流水主键集合
     * @return 结果
     */
    public int deleteTransactionRecordsByIds(Long[] ids);

    /**
     * 删除微信支付宝流水信息
     *
     * @param id 微信支付宝流水主键
     * @return 结果
     */
    public int deleteTransactionRecordsById(Long id);
}
