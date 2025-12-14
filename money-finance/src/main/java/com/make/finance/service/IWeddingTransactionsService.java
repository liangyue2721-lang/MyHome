package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.WeddingTransactions;

/**
 * 婚礼收支明细Service接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface IWeddingTransactionsService {
    /**
     * 查询婚礼收支明细
     *
     * @param id 婚礼收支明细主键
     * @return 婚礼收支明细
     */
    public WeddingTransactions selectWeddingTransactionsById(Long id);

    /**
     * 查询婚礼收支明细列表
     *
     * @param weddingTransactions 婚礼收支明细
     * @return 婚礼收支明细集合
     */
    public List<WeddingTransactions> selectWeddingTransactionsList(WeddingTransactions weddingTransactions);

    /**
     * 新增婚礼收支明细
     *
     * @param weddingTransactions 婚礼收支明细
     * @return 结果
     */
    public int insertWeddingTransactions(WeddingTransactions weddingTransactions);

    /**
     * 修改婚礼收支明细
     *
     * @param weddingTransactions 婚礼收支明细
     * @return 结果
     */
    public int updateWeddingTransactions(WeddingTransactions weddingTransactions);

    /**
     * 批量删除婚礼收支明细
     *
     * @param ids 需要删除的婚礼收支明细主键集合
     * @return 结果
     */
    public int deleteWeddingTransactionsByIds(Long[] ids);

    /**
     * 删除婚礼收支明细信息
     *
     * @param id 婚礼收支明细主键
     * @return 结果
     */
    public int deleteWeddingTransactionsById(Long id);
}
