package com.make.finance.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.WeddingExpensesMapper;
import com.make.finance.domain.WeddingExpenses;
import com.make.finance.service.IWeddingExpensesService;

/**
 * 婚礼订婚支出流水Service业务层处理
 *
 * @author erqi
 * @date 2026-02-16
 */
@Service
public class WeddingExpensesServiceImpl implements IWeddingExpensesService {

    @Autowired
    private WeddingExpensesMapper weddingExpensesMapper;

    /**
     * 查询婚礼订婚支出流水统计
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 婚礼订婚支出流水统计
     */
    @Override
    public List<Map<String, Object>> selectWeddingExpensesStats(WeddingExpenses weddingExpenses) {
        return weddingExpensesMapper.selectWeddingExpensesStats(weddingExpenses);
    }

    /**
     * 查询婚礼订婚支出流水
     *
     * @param id 婚礼订婚支出流水主键
     * @return 婚礼订婚支出流水
     */
    @Override
    public WeddingExpenses selectWeddingExpensesById(String id) {
        return weddingExpensesMapper.selectWeddingExpensesById(id);
    }

    /**
     * 查询婚礼订婚支出流水列表
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 婚礼订婚支出流水
     */
    @Override
    public List<WeddingExpenses> selectWeddingExpensesList(WeddingExpenses weddingExpenses) {
        return weddingExpensesMapper.selectWeddingExpensesList(weddingExpenses);
    }

    /**
     * 新增婚礼订婚支出流水
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 结果
     */
    @Override
    public int insertWeddingExpenses(WeddingExpenses weddingExpenses) {
        return weddingExpensesMapper.insertWeddingExpenses(weddingExpenses);
    }

    /**
     * 修改婚礼订婚支出流水
     *
     * @param weddingExpenses 婚礼订婚支出流水
     * @return 结果
     */
    @Override
    public int updateWeddingExpenses(WeddingExpenses weddingExpenses) {
        return weddingExpensesMapper.updateWeddingExpenses(weddingExpenses);
    }

    /**
     * 批量删除婚礼订婚支出流水
     *
     * @param ids 需要删除的婚礼订婚支出流水主键
     * @return 结果
     */
    @Override
    public int deleteWeddingExpensesByIds(String[] ids) {
        return weddingExpensesMapper.deleteWeddingExpensesByIds(ids);
    }

    /**
     * 删除婚礼订婚支出流水信息
     *
     * @param id 婚礼订婚支出流水主键
     * @return 结果
     */
    @Override
    public int deleteWeddingExpensesById(String id) {
        return weddingExpensesMapper.deleteWeddingExpensesById(id);
    }
}
