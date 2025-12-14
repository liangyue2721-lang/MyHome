package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.HouseExpenseMapper;
import com.make.finance.domain.HouseExpense;
import com.make.finance.service.IHouseExpenseService;

/**
 * 买房支出记录Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class HouseExpenseServiceImpl implements IHouseExpenseService {
    @Autowired
    private HouseExpenseMapper houseExpenseMapper;

    /**
     * 查询买房支出记录
     *
     * @param id 买房支出记录主键
     * @return 买房支出记录
     */
    @Override
    public HouseExpense selectHouseExpenseById(Long id) {
        return houseExpenseMapper.selectHouseExpenseById(id);
    }

    /**
     * 查询买房支出记录列表
     *
     * @param houseExpense 买房支出记录
     * @return 买房支出记录
     */
    @Override
    public List<HouseExpense> selectHouseExpenseList(HouseExpense houseExpense) {
        return houseExpenseMapper.selectHouseExpenseList(houseExpense);
    }

    /**
     * 新增买房支出记录
     *
     * @param houseExpense 买房支出记录
     * @return 结果
     */
    @Override
    public int insertHouseExpense(HouseExpense houseExpense) {
        return houseExpenseMapper.insertHouseExpense(houseExpense);
    }

    /**
     * 修改买房支出记录
     *
     * @param houseExpense 买房支出记录
     * @return 结果
     */
    @Override
    public int updateHouseExpense(HouseExpense houseExpense) {
        return houseExpenseMapper.updateHouseExpense(houseExpense);
    }

    /**
     * 批量删除买房支出记录
     *
     * @param ids 需要删除的买房支出记录主键
     * @return 结果
     */
    @Override
    public int deleteHouseExpenseByIds(Long[] ids) {
        return houseExpenseMapper.deleteHouseExpenseByIds(ids);
    }

    /**
     * 删除买房支出记录信息
     *
     * @param id 买房支出记录主键
     * @return 结果
     */
    @Override
    public int deleteHouseExpenseById(Long id) {
        return houseExpenseMapper.deleteHouseExpenseById(id);
    }
}
