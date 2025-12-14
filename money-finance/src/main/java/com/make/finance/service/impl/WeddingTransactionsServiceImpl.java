package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.WeddingTransactionsMapper;
import com.make.finance.domain.WeddingTransactions;
import com.make.finance.service.IWeddingTransactionsService;

/**
 * 婚礼收支明细Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class WeddingTransactionsServiceImpl implements IWeddingTransactionsService {
    @Autowired
    private WeddingTransactionsMapper weddingTransactionsMapper;

    /**
     * 查询婚礼收支明细
     *
     * @param id 婚礼收支明细主键
     * @return 婚礼收支明细
     */
    @Override
    public WeddingTransactions selectWeddingTransactionsById(Long id) {
        return weddingTransactionsMapper.selectWeddingTransactionsById(id);
    }

    /**
     * 查询婚礼收支明细列表
     *
     * @param weddingTransactions 婚礼收支明细
     * @return 婚礼收支明细
     */
    @Override
    public List<WeddingTransactions> selectWeddingTransactionsList(WeddingTransactions weddingTransactions) {
        return weddingTransactionsMapper.selectWeddingTransactionsList(weddingTransactions);
    }

    /**
     * 新增婚礼收支明细
     *
     * @param weddingTransactions 婚礼收支明细
     * @return 结果
     */
    @Override
    public int insertWeddingTransactions(WeddingTransactions weddingTransactions) {
        return weddingTransactionsMapper.insertWeddingTransactions(weddingTransactions);
    }

    /**
     * 修改婚礼收支明细
     *
     * @param weddingTransactions 婚礼收支明细
     * @return 结果
     */
    @Override
    public int updateWeddingTransactions(WeddingTransactions weddingTransactions) {
        return weddingTransactionsMapper.updateWeddingTransactions(weddingTransactions);
    }

    /**
     * 批量删除婚礼收支明细
     *
     * @param ids 需要删除的婚礼收支明细主键
     * @return 结果
     */
    @Override
    public int deleteWeddingTransactionsByIds(Long[] ids) {
        return weddingTransactionsMapper.deleteWeddingTransactionsByIds(ids);
    }

    /**
     * 删除婚礼收支明细信息
     *
     * @param id 婚礼收支明细主键
     * @return 结果
     */
    @Override
    public int deleteWeddingTransactionsById(Long id) {
        return weddingTransactionsMapper.deleteWeddingTransactionsById(id);
    }
}
