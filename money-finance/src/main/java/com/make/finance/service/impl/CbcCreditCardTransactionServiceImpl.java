package com.make.finance.service.impl;

import java.util.Date;
import java.util.List;

import com.make.common.utils.DateUtils;
import com.make.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.CbcCreditCardTransactionMapper;
import com.make.finance.domain.CbcCreditCardTransaction;
import com.make.finance.service.ICbcCreditCardTransactionService;

/**
 * 建行信用卡交易记录Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-26
 */
@Service
public class CbcCreditCardTransactionServiceImpl implements ICbcCreditCardTransactionService {
    @Autowired
    private CbcCreditCardTransactionMapper cbcCreditCardTransactionMapper;

    /**
     * 查询建行信用卡交易记录
     *
     * @param id 建行信用卡交易记录主键
     * @return 建行信用卡交易记录
     */
    @Override
    public CbcCreditCardTransaction selectCbcCreditCardTransactionById(Long id) {
        return cbcCreditCardTransactionMapper.selectCbcCreditCardTransactionById(id);
    }

    /**
     * 查询建行信用卡交易记录列表
     *
     * @param cbcCreditCardTransaction 建行信用卡交易记录
     * @return 建行信用卡交易记录
     */
    @Override
    public List<CbcCreditCardTransaction> selectCbcCreditCardTransactionList(CbcCreditCardTransaction cbcCreditCardTransaction) {
        return cbcCreditCardTransactionMapper.selectCbcCreditCardTransactionList(cbcCreditCardTransaction);
    }

    /**
     * 新增建行信用卡交易记录
     *
     * @param cbcCreditCardTransaction 建行信用卡交易记录
     * @return 结果
     */
    @Override
    public int insertCbcCreditCardTransaction(CbcCreditCardTransaction cbcCreditCardTransaction) {
        cbcCreditCardTransaction.setCreateTime(DateUtils.getNowDate());
        return cbcCreditCardTransactionMapper.insertCbcCreditCardTransaction(cbcCreditCardTransaction);
    }

    /**
     * 修改建行信用卡交易记录
     *
     * @param cbcCreditCardTransaction 建行信用卡交易记录
     * @return 结果
     */
    @Override
    public int updateCbcCreditCardTransaction(CbcCreditCardTransaction cbcCreditCardTransaction) {
        cbcCreditCardTransaction.setUpdateTime(DateUtils.getNowDate());
        return cbcCreditCardTransactionMapper.updateCbcCreditCardTransaction(cbcCreditCardTransaction);
    }

    /**
     * 批量删除建行信用卡交易记录
     *
     * @param ids 需要删除的建行信用卡交易记录主键
     * @return 结果
     */
    @Override
    public int deleteCbcCreditCardTransactionByIds(Long[] ids) {
        return cbcCreditCardTransactionMapper.deleteCbcCreditCardTransactionByIds(ids);
    }

    /**
     * 删除建行信用卡交易记录信息
     *
     * @param id 建行信用卡交易记录主键
     * @return 结果
     */
    @Override
    public int deleteCbcCreditCardTransactionById(Long id) {
        return cbcCreditCardTransactionMapper.deleteCbcCreditCardTransactionById(id);
    }

    /**
     * 批量新增建行信用卡交易记录
     *
     * @param cbcCreditCardTransactions 建行信用卡交易记录列表
     * @return 插入成功的记录数
     */
    @Override
    public int batchInsertCbcCreditCardTransaction(List<CbcCreditCardTransaction> cbcCreditCardTransactions) {
        if (cbcCreditCardTransactions == null || cbcCreditCardTransactions.isEmpty()) {
            return 0;
        }

        // 为每条记录设置创建时间
        Date now = DateUtils.getNowDate();
        cbcCreditCardTransactions.forEach(transaction -> {
            transaction.setCreateTime(now);
            transaction.setUpdateTime(now);
            transaction.setUserId(SecurityUtils.getUserId());
        });

        return cbcCreditCardTransactionMapper.batchInsertCbcCreditCardTransaction(cbcCreditCardTransactions);
    }
}
