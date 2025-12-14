package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.StockInfoDongfang;
import com.make.stock.domain.StockYearlyPerformance;

/**
 * 股票当年现数据Service接口
 *
 * @author erqi
 * @date 2025-10-19
 */
public interface IStockYearlyPerformanceService {

    /**
     * 查询股票当年现数据
     *
     * @param id 股票当年现数据主键
     * @return 股票当年现数据
     */
    public StockYearlyPerformance selectStockYearlyPerformanceById(Long id);

    /**
     * 查询股票当年现数据列表
     *
     * @param stockYearlyPerformance 股票当年现数据
     * @return 股票当年现数据集合
     */
    public List<StockYearlyPerformance> selectStockYearlyPerformanceList(StockYearlyPerformance stockYearlyPerformance);

    /**
     * 新增股票当年现数据
     *
     * @param stockYearlyPerformance 股票当年现数据
     * @return 结果
     */
    public int insertStockYearlyPerformance(StockYearlyPerformance stockYearlyPerformance);

    /**
     * 修改股票当年现数据
     *
     * @param stockYearlyPerformance 股票当年现数据
     * @return 结果
     */
    public int updateStockYearlyPerformance(StockYearlyPerformance stockYearlyPerformance);

    /**
     * 批量删除股票当年现数据
     *
     * @param ids 需要删除的股票当年现数据主键集合
     * @return 结果
     */
    public int deleteStockYearlyPerformanceByIds(Long[] ids);

    /**
     * 删除股票当年现数据信息
     *
     * @param id 股票当年现数据主键
     * @return 结果
     */
    public int deleteStockYearlyPerformanceById(Long id);

    List<StockYearlyPerformance> queryIDByCodes(List<String> allStockCodesList);
}
