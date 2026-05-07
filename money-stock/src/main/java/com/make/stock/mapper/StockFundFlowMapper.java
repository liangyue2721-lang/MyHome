package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockFundFlow;

/**
 * 主力资金流向Mapper接口
 *
 * @author erqi
 * @date 2026-05-07
 */
public interface StockFundFlowMapper {

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
     * 查询主力资金流向列表 (聚合计算多日)
     *
     * @param stockFundFlow 查询条件
     * @return 主力资金流向聚合集合
     */
    public List<StockFundFlow> selectStockFundFlowListAggregated(StockFundFlow stockFundFlow);

    /**
     * 查询指定天数内单只股票上榜Top10的次数
     *
     * @param rankDays 天数
     * @return 包含股票代码、名称和出现次数的列表
     */
    /**
     * 查询大单流入小单流出背离Top10
     *
     * @return 结果列表
     */
    public List<java.util.Map<String, Object>> selectDivergenceLargeInSmallOut();

    /**
     * 查询大单流出小单流入背离Top10
     *
     * @return 结果列表
     */
    public List<java.util.Map<String, Object>> selectDivergenceLargeOutSmallIn();

    public List<java.util.Map<String, Object>> selectTop10Appearances(@org.apache.ibatis.annotations.Param("rankDays") Integer rankDays);

    /**
     * 查询指定股票列表的最新资金流向数据
     *
     * @param stockCodes 股票代码列表
     * @return 最新资金流向数据列表
     */
    public List<StockFundFlow> selectLatestFundFlowByCodes(@org.apache.ibatis.annotations.Param("stockCodes") List<String> stockCodes);

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
     * 删除主力资金流向
     *
     * @param id 主力资金流向主键
     * @return 结果
     */
    public int deleteStockFundFlowById(String id);

    /**
     * 批量删除主力资金流向
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockFundFlowByIds(String[] ids);
}
