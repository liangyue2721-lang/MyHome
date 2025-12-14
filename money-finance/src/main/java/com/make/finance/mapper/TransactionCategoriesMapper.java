package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.TransactionCategories;

/**
 * 交易分类关键词Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface TransactionCategoriesMapper {
    /**
     * 查询交易分类关键词
     *
     * @param id 交易分类关键词主键
     * @return 交易分类关键词
     */
    public TransactionCategories selectTransactionCategoriesById(Long id);

    /**
     * 查询交易分类关键词列表
     *
     * @param transactionCategories 交易分类关键词
     * @return 交易分类关键词集合
     */
    public List<TransactionCategories> selectTransactionCategoriesList(TransactionCategories transactionCategories);

    /**
     * 新增交易分类关键词
     *
     * @param transactionCategories 交易分类关键词
     * @return 结果
     */
    public int insertTransactionCategories(TransactionCategories transactionCategories);

    /**
     * 修改交易分类关键词
     *
     * @param transactionCategories 交易分类关键词
     * @return 结果
     */
    public int updateTransactionCategories(TransactionCategories transactionCategories);

    /**
     * 删除交易分类关键词
     *
     * @param id 交易分类关键词主键
     * @return 结果
     */
    public int deleteTransactionCategoriesById(Long id);

    /**
     * 批量删除交易分类关键词
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTransactionCategoriesByIds(Long[] ids);
}
