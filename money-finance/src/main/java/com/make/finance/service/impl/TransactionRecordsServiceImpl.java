package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.TransactionRecordsMapper;
import com.make.finance.domain.TransactionRecords;
import com.make.finance.service.ITransactionRecordsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 微信支付宝流水Service业务层处理
 *
 * @author è´°æ
 * @date 2025-05-27
 */
@Service
public class TransactionRecordsServiceImpl implements ITransactionRecordsService {
    @Autowired
    private TransactionRecordsMapper transactionRecordsMapper;

    /**
     * 查询微信支付宝流水
     *
     * @param id 微信支付宝流水主键
     * @return 微信支付宝流水
     */
    @Override
    public TransactionRecords selectTransactionRecordsById(Long id) {
        return transactionRecordsMapper.selectTransactionRecordsById(id);
    }

    /**
     * 查询微信支付宝流水列表
     *
     * @param transactionRecords 微信支付宝流水
     * @return 微信支付宝流水
     */
    @Override
    public List<TransactionRecords> selectTransactionRecordsList(TransactionRecords transactionRecords) {
        return transactionRecordsMapper.selectTransactionRecordsList(transactionRecords);
    }

    /**
     * 新增微信支付宝流水
     *
     * @param transactionRecords 微信支付宝流水
     * @return 结果
     */
    @Override
    public int insertTransactionRecords(TransactionRecords transactionRecords) {
        return transactionRecordsMapper.insertTransactionRecords(transactionRecords);
    }

    /**
     * 修改微信支付宝流水
     *
     * @param transactionRecords 微信支付宝流水
     * @return 结果
     */
    @Override
    public int updateTransactionRecords(TransactionRecords transactionRecords) {
        return transactionRecordsMapper.updateTransactionRecords(transactionRecords);
    }

    /**
     * 批量删除微信支付宝流水
     *
     * @param ids 需要删除的微信支付宝流水主键
     * @return 结果
     */
    @Override
    public int deleteTransactionRecordsByIds(Long[] ids) {
        return transactionRecordsMapper.deleteTransactionRecordsByIds(ids);
    }

    /**
     * 删除微信支付宝流水信息
     *
     * @param id 微信支付宝流水主键
     * @return 结果
     */
    @Override
    public int deleteTransactionRecordsById(Long id) {
        return transactionRecordsMapper.deleteTransactionRecordsById(id);
    }

    /**
     * 批量插入交易记录
     */
    @Override
    @Transactional
    public int insertTransactionRecordsBatch(List<TransactionRecords> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        return transactionRecordsMapper.insertBatch(records);
    }
}
