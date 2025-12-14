package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.InvestmentRecords;

/**
 * 投资利润回报记录Service接口
 *
 * @author erqi
 * @date 2025-07-05
 */
public interface IInvestmentRecordsService {

    /**
     * 查询投资利润回报记录
     *
     * @param id 投资利润回报记录主键
     * @return 投资利润回报记录
     */
    public InvestmentRecords selectInvestmentRecordsById(Long id);

    /**
     * 查询投资利润回报记录列表
     *
     * @param investmentRecords 投资利润回报记录
     * @return 投资利润回报记录集合
     */
    public List<InvestmentRecords> selectInvestmentRecordsList(InvestmentRecords investmentRecords);

    /**
     * 新增投资利润回报记录
     *
     * @param investmentRecords 投资利润回报记录
     * @return 结果
     */
    public int insertInvestmentRecords(InvestmentRecords investmentRecords);

    /**
     * 修改投资利润回报记录
     *
     * @param investmentRecords 投资利润回报记录
     * @return 结果
     */
    public int updateInvestmentRecords(InvestmentRecords investmentRecords);

    /**
     * 批量删除投资利润回报记录
     *
     * @param ids 需要删除的投资利润回报记录主键集合
     * @return 结果
     */
    public int deleteInvestmentRecordsByIds(Long[] ids);

    /**
     * 删除投资利润回报记录信息
     *
     * @param id 投资利润回报记录主键
     * @return 结果
     */
    public int deleteInvestmentRecordsById(Long id);
}
