package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockInfoDongfang;
import com.make.stock.domain.StockYearlyPerformance;
import org.apache.ibatis.annotations.Param;

/**
 * 股票当年现数据Mapper接口
 *
 * @author erqi
 * @date 2025-10-19
 */
public interface StockYearlyPerformanceMapper {

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
     * 删除股票当年现数据
     *
     * @param id 股票当年现数据主键
     * @return 结果
     */
    public int deleteStockYearlyPerformanceById(Long id);

    /**
     * 批量删除股票当年现数据
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockYearlyPerformanceByIds(Long[] ids);

    List<StockYearlyPerformance> selectBatchByCodes(@Param("codes") List<String> stockCodes);
}
