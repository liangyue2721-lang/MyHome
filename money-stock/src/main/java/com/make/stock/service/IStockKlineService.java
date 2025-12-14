package com.make.stock.service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import com.make.stock.domain .StockKline;

/**
 * 股票K线数据Service接口
 *
 * @author erqi
 * @date 2025-11-03
 */
public interface IStockKlineService {

    /**
     * 查询股票K线数据
     *
     * @param id 股票K线数据主键
     * @return 股票K线数据
     */
    public StockKline selectStockKlineById(String id);

    /**
     * 查询股票K线数据列表
     *
     * @param stockKline 股票K线数据
     * @return 股票K线数据集合
     */
    public List<StockKline> selectStockKlineList(StockKline stockKline);

    /**
     * 新增股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    public int insertStockKline(StockKline stockKline);

    /**
     * 修改股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    public int updateStockKline(StockKline stockKline);

    /**
     * 批量删除股票K线数据
     *
     * @param ids 需要删除的股票K线数据主键集合
     * @return 结果
     */
    public int deleteStockKlineByIds(String[] ids);

    /**
     * 删除股票K线数据信息
     *
     * @param id 股票K线数据主键
     * @return 结果
     */
    public int deleteStockKlineById(String id);

    /**
     * 根据股票代码和交易日期判断是否存在记录
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日期
     * @return 是否存在记录
     */
    boolean existsByStockAndDate(String stockCode, Date tradeDate);

    /**
     * 根据股票代码和交易日期更新股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    public int updateByStockCodeAndTradeDate(StockKline stockKline);

    /**
     * 批量插入或更新股票K线数据
     *
     * @param klines 股票K线数据列表
     * @return 影响的行数
     */
    public int insertOrUpdateBatch(List<StockKline> klines);

    void batchUpdateByStockCodeAndTradeDate(List<StockKline> updateList);

    List<LocalDate> selectExistsDates(String stockCode, List<LocalDate> tradeDateList);
}
