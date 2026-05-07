package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.StockFundFlow;

/**
 * 主力资金流向Service接口
 *
 * @author erqi
 * @date 2026-05-07
 */
public interface IStockFundFlowService {

    /**
     * 查询主力资金流向
     *
     * @param id 主力资金流向主键
     * @return 主力资金流向
     */
    public StockFundFlow selectStockFundFlowById(String id);

    /**
     * 查询主力资金流向列表
     *
     * @param stockFundFlow 主力资金流向
     * @return 主力资金流向集合
     */
    public List<StockFundFlow> selectStockFundFlowList(StockFundFlow stockFundFlow);

    /**
     * 获取多维度的排行图表数据
     *
     * @return 包含 3日、5日、季度(60日)、年度(250日) 的Top10上榜次数数据
     */
    public java.util.Map<String, List<java.util.Map<String, Object>>> selectChartData();

    /**
     * 获取大小单背离图表数据
     *
     * @return 包含 大单流入小单流出、大单流出小单流入 两种背离数据的Top10
     */
    public java.util.Map<String, List<java.util.Map<String, Object>>> selectDivergenceData();

    /**
     * 新增主力资金流向
     *
     * @param stockFundFlow 主力资金流向
     * @return 结果
     */
    public int insertStockFundFlow(StockFundFlow stockFundFlow);

    /**
     * 修改主力资金流向
     *
     * @param stockFundFlow 主力资金流向
     * @return 结果
     */
    public int updateStockFundFlow(StockFundFlow stockFundFlow);

    /**
     * 批量删除主力资金流向
     *
     * @param ids 需要删除的主力资金流向主键集合
     * @return 结果
     */
    public int deleteStockFundFlowByIds(String[] ids);

    /**
     * 删除主力资金流向信息
     *
     * @param id 主力资金流向主键
     * @return 结果
     */
    public int deleteStockFundFlowById(String id);
}
