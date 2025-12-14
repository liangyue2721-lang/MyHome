package com.make.stock.service.impl;

import java.util.Collections;
import java.util.List;

import com.make.common.utils.DateUtils;
import com.make.stock.domain.StockInfoDongfang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockYearlyPerformanceMapper;
import com.make.stock.domain.StockYearlyPerformance;
import com.make.stock.service.IStockYearlyPerformanceService;

/**
 * 股票当年现数据Service业务层处理
 *
 * @author erqi
 * @date 2025-10-19
 */
@Service
public class StockYearlyPerformanceServiceImpl implements IStockYearlyPerformanceService {

    @Autowired
    private StockYearlyPerformanceMapper stockYearlyPerformanceMapper;

    /**
     * 查询股票当年现数据
     *
     * @param id 股票当年现数据主键
     * @return 股票当年现数据
     */
    @Override
    public StockYearlyPerformance selectStockYearlyPerformanceById(Long id) {
        return stockYearlyPerformanceMapper.selectStockYearlyPerformanceById(id);
    }

    /**
     * 查询股票当年现数据列表
     *
     * @param stockYearlyPerformance 股票当年现数据
     * @return 股票当年现数据
     */
    @Override
    public List<StockYearlyPerformance> selectStockYearlyPerformanceList(StockYearlyPerformance stockYearlyPerformance) {
        return stockYearlyPerformanceMapper.selectStockYearlyPerformanceList(stockYearlyPerformance);
    }

    /**
     * 新增股票当年现数据
     *
     * @param stockYearlyPerformance 股票当年现数据
     * @return 结果
     */
    @Override
    public int insertStockYearlyPerformance(StockYearlyPerformance stockYearlyPerformance) {
        stockYearlyPerformance.setCreateTime(DateUtils.getNowDate());
        return stockYearlyPerformanceMapper.insertStockYearlyPerformance(stockYearlyPerformance);
    }

    /**
     * 修改股票当年现数据
     *
     * @param stockYearlyPerformance 股票当年现数据
     * @return 结果
     */
    @Override
    public int updateStockYearlyPerformance(StockYearlyPerformance stockYearlyPerformance) {
        stockYearlyPerformance.setUpdateTime(DateUtils.getNowDate());
        return stockYearlyPerformanceMapper.updateStockYearlyPerformance(stockYearlyPerformance);
    }

    /**
     * 批量删除股票当年现数据
     *
     * @param ids 需要删除的股票当年现数据主键
     * @return 结果
     */
    @Override
    public int deleteStockYearlyPerformanceByIds(Long[] ids) {
        return stockYearlyPerformanceMapper.deleteStockYearlyPerformanceByIds(ids);
    }

    /**
     * 删除股票当年现数据信息
     *
     * @param id 股票当年现数据主键
     * @return 结果
     */
    @Override
    public int deleteStockYearlyPerformanceById(Long id) {
        return stockYearlyPerformanceMapper.deleteStockYearlyPerformanceById(id);
    }

    @Override
    public List<StockYearlyPerformance> queryIDByCodes(List<String> allStockCodesList) {
        return stockYearlyPerformanceMapper.selectBatchByCodes(allStockCodesList);
    }
}
