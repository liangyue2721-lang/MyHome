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
        return stockFundFlowMapper.selectStockFundFlowList(stockFundFlow);
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
