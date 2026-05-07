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
