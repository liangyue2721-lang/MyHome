package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.MonthlyBillsMapper;
import com.make.finance.domain.MonthlyBills;
import com.make.finance.service.IMonthlyBillsService;

/**
 * 月度账单 (单JSON架构)Service业务层处理
 *
 * @author erqi
 * @date 2026-04-01
 */
@Service
public class MonthlyBillsServiceImpl implements IMonthlyBillsService {

    @Autowired
    private MonthlyBillsMapper monthlyBillsMapper;

    /**
     * 查询月度账单 (单JSON架构)
     *
     * @param id 月度账单 (单JSON架构)主键
     * @return 月度账单 (单JSON架构)
     */
    @Override
    public MonthlyBills selectMonthlyBillsById(Long id) {
        return monthlyBillsMapper.selectMonthlyBillsById(id);
    }

    /**
     * 查询月度账单 (单JSON架构)列表
     *
     * @param monthlyBills 月度账单 (单JSON架构)
     * @return 月度账单 (单JSON架构)
     */
    @Override
    public List<MonthlyBills> selectMonthlyBillsList(MonthlyBills monthlyBills) {
        return monthlyBillsMapper.selectMonthlyBillsList(monthlyBills);
    }

    /**
     * 新增月度账单 (单JSON架构)
     *
     * @param monthlyBills 月度账单 (单JSON架构)
     * @return 结果
     */
    @Override
    public int insertMonthlyBills(MonthlyBills monthlyBills) {
        return monthlyBillsMapper.insertMonthlyBills(monthlyBills);
    }

    /**
     * 修改月度账单 (单JSON架构)
     *
     * @param monthlyBills 月度账单 (单JSON架构)
     * @return 结果
     */
    @Override
    public int updateMonthlyBills(MonthlyBills monthlyBills) {
        return monthlyBillsMapper.updateMonthlyBills(monthlyBills);
    }

    /**
     * 批量删除月度账单 (单JSON架构)
     *
     * @param ids 需要删除的月度账单 (单JSON架构)主键
     * @return 结果
     */
    @Override
    public int deleteMonthlyBillsByIds(Long[] ids) {
        return monthlyBillsMapper.deleteMonthlyBillsByIds(ids);
    }

    /**
     * 删除月度账单 (单JSON架构)信息
     *
     * @param id 月度账单 (单JSON架构)主键
     * @return 结果
     */
    @Override
    public int deleteMonthlyBillsById(Long id) {
        return monthlyBillsMapper.deleteMonthlyBillsById(id);
    }
}
