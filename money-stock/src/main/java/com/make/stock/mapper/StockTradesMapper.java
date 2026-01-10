package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockTrades;

/**
 * 股票利润Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface StockTradesMapper {
    /**
     * 查询股票利润
     *
     * @param id 股票利润主键
     * @return 股票利润
     */
    public StockTrades selectStockTradesById(Long id);

    /**
     * 查询股票利润列表
     *
     * @param stockTrades 股票利润
     * @return 股票利润集合
     */
    public List<StockTrades> selectStockTradesList(StockTrades stockTrades);

    /**
     * 新增股票利润
     *
     * @param stockTrades 股票利润
     * @return 结果
     */
    public int insertStockTrades(StockTrades stockTrades);

    /**
     * 修改股票利润
     *
     * @param stockTrades 股票利润
     * @return 结果
     */
    public int updateStockTrades(StockTrades stockTrades);

    /**
     * 删除股票利润
     *
     * @param id 股票利润主键
     * @return 结果
     */
    public int deleteStockTradesById(Long id);

    /**
     * 批量删除股票利润
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockTradesByIds(Long[] ids);


    /**
     * 根据名称修改股票最新利润
     *
     * @param stockTrades 股票最新数据
     * @return 结果
     */
    public int updateStockTradesByCode(StockTrades stockTrades);

    /**
     * 查询股票信息
     *
     * @param stockTrades 股票信息
     * @return 股票信息集合
     */
    List<StockTrades> selectStockTradesOne(StockTrades stockTrades);

    int updateStockTradesBatch(List<StockTrades> unsyncedTrades);

    // 在 StockTradesService 接口中添加方法
    List<StockTrades> selectStockTradesByYear(int year);
}
