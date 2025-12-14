package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.TransactionCategoriesMapper;
import com.make.finance.domain.TransactionCategories;
import com.make.finance.service.ITransactionCategoriesService;

/**
 * 交易分类关键词Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class TransactionCategoriesServiceImpl implements ITransactionCategoriesService {
    @Autowired
    private TransactionCategoriesMapper transactionCategoriesMapper;

    /**
     * 查询交易分类关键词
     *
     * @param id 交易分类关键词主键
     * @return 交易分类关键词
     */
    @Override
    public TransactionCategories selectTransactionCategoriesById(Long id) {
        return transactionCategoriesMapper.selectTransactionCategoriesById(id);
    }

    /**
     * 查询交易分类关键词列表
     *
     * @param transactionCategories 交易分类关键词
     * @return 交易分类关键词
     */
    @Override
    public List<TransactionCategories> selectTransactionCategoriesList(TransactionCategories transactionCategories) {
        return transactionCategoriesMapper.selectTransactionCategoriesList(transactionCategories);
    }

    /**
     * 新增交易分类关键词
     *
     * @param transactionCategories 交易分类关键词
     * @return 结果
     */
    @Override
    public int insertTransactionCategories(TransactionCategories transactionCategories) {
        return transactionCategoriesMapper.insertTransactionCategories(transactionCategories);
    }

    /**
     * 修改交易分类关键词
     *
     * @param transactionCategories 交易分类关键词
     * @return 结果
     */
    @Override
    public int updateTransactionCategories(TransactionCategories transactionCategories) {
        return transactionCategoriesMapper.updateTransactionCategories(transactionCategories);
    }

    /**
     * 批量删除交易分类关键词
     *
     * @param ids 需要删除的交易分类关键词主键
     * @return 结果
     */
    @Override
    public int deleteTransactionCategoriesByIds(Long[] ids) {
        return transactionCategoriesMapper.deleteTransactionCategoriesByIds(ids);
    }

    /**
     * 删除交易分类关键词信息
     *
     * @param id 交易分类关键词主键
     * @return 结果
     */
    @Override
    public int deleteTransactionCategoriesById(Long id) {
        return transactionCategoriesMapper.deleteTransactionCategoriesById(id);
    }
}
