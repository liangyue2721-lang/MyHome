package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.WeddingExpense;

/**
 * 婚礼支出记录Service接口
 *
 * @author erqi
 * @date 2025-12-17
 */
public interface IWeddingExpenseService {

    /**
     * 查询婚礼支出记录
     *
     * @param id 婚礼支出记录主键
     * @return 婚礼支出记录
     */
    public WeddingExpense selectWeddingExpenseById(Long id);

    /**
     * 查询婚礼支出记录列表
     *
     * @param weddingExpense 婚礼支出记录
     * @return 婚礼支出记录集合
     */
    public List<WeddingExpense> selectWeddingExpenseList(WeddingExpense weddingExpense);

    /**
     * 新增婚礼支出记录
     *
     * @param weddingExpense 婚礼支出记录
     * @return 结果
     */
    public int insertWeddingExpense(WeddingExpense weddingExpense);

    /**
     * 修改婚礼支出记录
     *
     * @param weddingExpense 婚礼支出记录
     * @return 结果
     */
    public int updateWeddingExpense(WeddingExpense weddingExpense);

    /**
     * 批量删除婚礼支出记录
     *
     * @param ids 需要删除的婚礼支出记录主键集合
     * @return 结果
     */
    public int deleteWeddingExpenseByIds(Long[] ids);

    /**
     * 删除婚礼支出记录信息
     *
     * @param id 婚礼支出记录主键
     * @return 结果
     */
    public int deleteWeddingExpenseById(Long id);
}
