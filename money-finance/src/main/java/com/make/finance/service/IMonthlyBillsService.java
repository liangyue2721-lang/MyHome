package com.make.finance.service;

import java.util.List;

import com.make.finance.domain .MonthlyBills;

/**
 * 月度账单 (单JSON架构)Service接口
 *
 * @author erqi
 * @date 2026-04-01
 */
public interface IMonthlyBillsService {

    /**
     * 查询月度账单 (单JSON架构)
     *
     * @param id 月度账单 (单JSON架构)主键
     * @return 月度账单 (单JSON架构)
     */
    public MonthlyBills selectMonthlyBillsById(Long id);

    /**
     * 查询月度账单 (单JSON架构)列表
     *
     * @param monthlyBills 月度账单 (单JSON架构)
     * @return 月度账单 (单JSON架构)集合
     */
    public List<MonthlyBills> selectMonthlyBillsList(MonthlyBills monthlyBills);

    /**
     * 新增月度账单 (单JSON架构)
     *
     * @param monthlyBills 月度账单 (单JSON架构)
     * @return 结果
     */
    public int insertMonthlyBills(MonthlyBills monthlyBills);

    /**
     * 修改月度账单 (单JSON架构)
     *
     * @param monthlyBills 月度账单 (单JSON架构)
     * @return 结果
     */
    public int updateMonthlyBills(MonthlyBills monthlyBills);

    /**
     * 批量删除月度账单 (单JSON架构)
     *
     * @param ids 需要删除的月度账单 (单JSON架构)主键集合
     * @return 结果
     */
    public int deleteMonthlyBillsByIds(Long[] ids);

    /**
     * 删除月度账单 (单JSON架构)信息
     *
     * @param id 月度账单 (单JSON架构)主键
     * @return 结果
     */
    public int deleteMonthlyBillsById(Long id);
}
