package com.make.stock.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockTickMapper;
import com.make.stock.domain.StockTick;
import com.make.stock.service.IStockTickService;

/**
 * 股票逐笔成交明细Service业务层处理
 *
 * @author erqi
 * @date 2026-01-27
 */
@Service
public class StockTickServiceImpl implements IStockTickService {

    @Autowired
    private StockTickMapper stockTickMapper;

    /**
     * 查询股票逐笔成交明细
     *
     * @param id 股票逐笔成交明细主键
     * @return 股票逐笔成交明细
     */
    @Override
    public StockTick selectStockTickById(String id) {
        return stockTickMapper.selectStockTickById(id);
    }

    /**
     * 查询股票逐笔成交明细列表
     *
     * @param stockTick 股票逐笔成交明细
     * @return 股票逐笔成交明细
     */
    @Override
    public List<StockTick> selectStockTickList(StockTick stockTick) {
        return stockTickMapper.selectStockTickList(stockTick);
    }

    /**
     * 新增股票逐笔成交明细
     *
     * @param stockTick 股票逐笔成交明细
     * @return 结果
     */
    @Override
    public int insertStockTick(StockTick stockTick) {
        stockTick.setCreateTime(DateUtils.getNowDate());
        return stockTickMapper.insertStockTick(stockTick);
    }

    /**
     * 修改股票逐笔成交明细
     *
     * @param stockTick 股票逐笔成交明细
     * @return 结果
     */
    @Override
    public int updateStockTick(StockTick stockTick) {
        stockTick.setUpdateTime(DateUtils.getNowDate());
        return stockTickMapper.updateStockTick(stockTick);
    }

    /**
     * 批量删除股票逐笔成交明细
     *
     * @param ids 需要删除的股票逐笔成交明细主键
     * @return 结果
     */
    @Override
    public int deleteStockTickByIds(String[] ids) {
        return stockTickMapper.deleteStockTickByIds(ids);
    }

    /**
     * 删除股票逐笔成交明细信息
     *
     * @param id 股票逐笔成交明细主键
     * @return 结果
     */
    @Override
    public int deleteStockTickById(String id) {
        return stockTickMapper.deleteStockTickById(id);
    }
}
