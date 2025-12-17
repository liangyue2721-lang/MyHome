package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.WeddingExpenseMapper;
import com.make.finance.domain.WeddingExpense;
import com.make.finance.service.IWeddingExpenseService;

/**
 * 婚礼支出记录Service业务层处理
 *
 * @author erqi
 * @date 2025-12-17
 */
@Service
public class WeddingExpenseServiceImpl implements IWeddingExpenseService {

    @Autowired
    private WeddingExpenseMapper weddingExpenseMapper;

    /**
     * 查询婚礼支出记录
     *
     * @param id 婚礼支出记录主键
     * @return 婚礼支出记录
     */
    @Override
    public WeddingExpense selectWeddingExpenseById(Long id) {
        return weddingExpenseMapper.selectWeddingExpenseById(id);
    }

    /**
     * 查询婚礼支出记录列表
     *
     * @param weddingExpense 婚礼支出记录
     * @return 婚礼支出记录
     */
    @Override
    public List<WeddingExpense> selectWeddingExpenseList(WeddingExpense weddingExpense) {
        return weddingExpenseMapper.selectWeddingExpenseList(weddingExpense);
    }

    /**
     * 新增婚礼支出记录
     *
     * @param weddingExpense 婚礼支出记录
     * @return 结果
     */
    @Override
    public int insertWeddingExpense(WeddingExpense weddingExpense) {
        return weddingExpenseMapper.insertWeddingExpense(weddingExpense);
    }

    /**
     * 修改婚礼支出记录
     *
     * @param weddingExpense 婚礼支出记录
     * @return 结果
     */
    @Override
    public int updateWeddingExpense(WeddingExpense weddingExpense) {
        return weddingExpenseMapper.updateWeddingExpense(weddingExpense);
    }

    /**
     * 批量删除婚礼支出记录
     *
     * @param ids 需要删除的婚礼支出记录主键
     * @return 结果
     */
    @Override
    public int deleteWeddingExpenseByIds(Long[] ids) {
        return weddingExpenseMapper.deleteWeddingExpenseByIds(ids);
    }

    /**
     * 删除婚礼支出记录信息
     *
     * @param id 婚礼支出记录主键
     * @return 结果
     */
    @Override
    public int deleteWeddingExpenseById(Long id) {
        return weddingExpenseMapper.deleteWeddingExpenseById(id);
    }
}
