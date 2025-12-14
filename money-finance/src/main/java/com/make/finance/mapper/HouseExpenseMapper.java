package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.HouseExpense;

/**
 * 买房支出记录Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface HouseExpenseMapper {
    /**
     * 查询买房支出记录
     *
     * @param id 买房支出记录主键
     * @return 买房支出记录
     */
    public HouseExpense selectHouseExpenseById(Long id);

    /**
     * 查询买房支出记录列表
     *
     * @param houseExpense 买房支出记录
     * @return 买房支出记录集合
     */
    public List<HouseExpense> selectHouseExpenseList(HouseExpense houseExpense);

    /**
     * 新增买房支出记录
     *
     * @param houseExpense 买房支出记录
     * @return 结果
     */
    public int insertHouseExpense(HouseExpense houseExpense);

    /**
     * 修改买房支出记录
     *
     * @param houseExpense 买房支出记录
     * @return 结果
     */
    public int updateHouseExpense(HouseExpense houseExpense);

    /**
     * 删除买房支出记录
     *
     * @param id 买房支出记录主键
     * @return 结果
     */
    public int deleteHouseExpenseById(Long id);

    /**
     * 批量删除买房支出记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHouseExpenseByIds(Long[] ids);
}
