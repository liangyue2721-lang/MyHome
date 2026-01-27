package com.make.stock.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.make.stock.domain.StockTick;

/**
 * 股票逐笔成交明细Mapper接口
 *
 * @author erqi
 * @date 2026-01-27
 */
public interface StockTickMapper {

    /**
     * 查询股票逐笔成交明细
     *
     * @param id 股票逐笔成交明细主键
     * @return 股票逐笔成交明细
     */
    public StockTick selectStockTickById(String id);

    /**
     * 查询股票逐笔成交明细列表
     *
     * @param stockTick 股票逐笔成交明细
     * @return 股票逐笔成交明细集合
     */
    public List<StockTick> selectStockTickList(StockTick stockTick);

    /**
     * 新增股票逐笔成交明细
     *
     * @param stockTick 股票逐笔成交明细
     * @return 结果
     */
    public int insertStockTick(StockTick stockTick);

    /**
     * 修改股票逐笔成交明细
     *
     * @param stockTick 股票逐笔成交明细
     * @return 结果
     */
    public int updateStockTick(StockTick stockTick);

    /**
     * 删除股票逐笔成交明细
     *
     * @param id 股票逐笔成交明细主键
     * @return 结果
     */
    public int deleteStockTickById(String id);

    /**
     * 批量删除股票逐笔成交明细
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockTickByIds(String[] ids);

    /**
     * 批量新增股票逐笔成交明细
     *
     * @param stockTickList 股票逐笔成交明细列表
     * @return 结果
     */
    public int insertStockTickBatch(List<StockTick> stockTickList);

    /**
     * 查询指定日期最大的 tickCount
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日期
     * @return 最大 tickCount
     */
    public Long selectMaxTickCount(@Param("stockCode") String stockCode, @Param("tradeDate") Date tradeDate);
}
