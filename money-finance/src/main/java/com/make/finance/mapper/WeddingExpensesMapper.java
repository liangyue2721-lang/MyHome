package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.WeddingExpenses;

/**
 * 婚礼订婚支出流水Mapper接口
 *
 * @author erqi
 * @date 2026-02-16
 */
public interface WeddingExpensesMapper {

    /**
     * 查询婚礼订婚支出流水
     *
     * @param id 婚礼订婚支出流水主键
     * @return 婚礼订婚支出流水
     */
    public WeddingExpenses selectWeddingExpensesById(String id);

    /**
     * 查询婚礼订婚支出流水列表
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 婚礼订婚支出流水集合
     */
    public List<WeddingExpenses> selectWeddingExpensesList(WeddingExpenses weddingExpenses);

    /**
     * 新增婚礼订婚支出流水
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 结果
     */
    public int insertWeddingExpenses(WeddingExpenses weddingExpenses);

    /**
     * 修改婚礼订婚支出流水
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 结果
     */
    public int updateWeddingExpenses(WeddingExpenses weddingExpenses);

    /**
     * 删除婚礼订婚支出流水
     *
     * @param id 婚礼订婚支出流水主键
     * @return 结果
     */
    public int deleteWeddingExpensesById(String id);

    /**
     * 批量删除婚礼订婚支出流水
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWeddingExpensesByIds(String[] ids);
}
