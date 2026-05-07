package com.make.stock.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockFundFlowMapper;
import com.make.stock.domain.StockFundFlow;
import com.make.stock.service.IStockFundFlowService;

/**
 * 主力资金流向Service业务层处理
 *
 * @author erqi
 * @date 2026-05-07
 */
@Service
public class StockFundFlowServiceImpl implements IStockFundFlowService {

    @Autowired
    private StockFundFlowMapper stockFundFlowMapper;

    /**
     * 查询主力资金流向
     *
     * @param id 主力资金流向主键
     * @return 主力资金流向
     */
    @Override
    public StockFundFlow selectStockFundFlowById(String id) {
        return stockFundFlowMapper.selectStockFundFlowById(id);
    }

    /**
     * 查询主力资金流向列表
     *
     * @param stockFundFlow 主力资金流向
     * @return 主力资金流向
     */
    @Override
    public List<StockFundFlow> selectStockFundFlowList(StockFundFlow stockFundFlow) {
        if (stockFundFlow.getRankDays() != null && stockFundFlow.getRankDays() > 1) {
            return stockFundFlowMapper.selectStockFundFlowListAggregated(stockFundFlow);
        }
        return stockFundFlowMapper.selectStockFundFlowList(stockFundFlow);
    }

    /**
     * 获取多维度的排行图表数据
     *
     * @return 包含 3日、5日、季度(60日)、年度(250日) 的Top10上榜次数数据
     */
    @Override
    public java.util.Map<String, List<java.util.Map<String, Object>>> selectChartData() {
        java.util.Map<String, List<java.util.Map<String, Object>>> result = new java.util.HashMap<>();
        result.put("day3", stockFundFlowMapper.selectTop10Appearances(3));
        result.put("day5", stockFundFlowMapper.selectTop10Appearances(5));
        result.put("quarter", stockFundFlowMapper.selectTop10Appearances(60));
        result.put("year", stockFundFlowMapper.selectTop10Appearances(250));
        return result;
    }

    /**
     * 新增主力资金流向
     *
     * @param stockFundFlow 主力资金流向
     * @return 结果
     */
    @Override
    public int insertStockFundFlow(StockFundFlow stockFundFlow) {
            return stockFundFlowMapper.insertStockFundFlow(stockFundFlow);
    }

    /**
     * 修改主力资金流向
     *
     * @param stockFundFlow 主力资金流向
     * @return 结果
     */
    @Override
    public int updateStockFundFlow(StockFundFlow stockFundFlow) {
        return stockFundFlowMapper.updateStockFundFlow(stockFundFlow);
    }

    /**
     * 批量删除主力资金流向
     *
     * @param ids 需要删除的主力资金流向主键
     * @return 结果
     */
    @Override
    public int deleteStockFundFlowByIds(String[] ids) {
        return stockFundFlowMapper.deleteStockFundFlowByIds(ids);
    }

    /**
     * 删除主力资金流向信息
     *
     * @param id 主力资金流向主键
     * @return 结果
     */
    @Override
    public int deleteStockFundFlowById(String id) {
        return stockFundFlowMapper.deleteStockFundFlowById(id);
    }
}
