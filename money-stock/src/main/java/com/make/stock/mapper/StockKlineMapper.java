package com.make.stock.mapper;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import com.make.stock.domain.StockKline;
import com.make.stock.domain.vo.StockRankingStat;
import org.apache.ibatis.annotations.Param;

/**
 * 股票K线数据Mapper接口
 *
 * @author erqi
 * @date 2025-11-03
 */
public interface StockKlineMapper {

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
     * 删除股票K线数据
     *
     * @param id 股票K线数据主键
     * @return 结果
     */
    public int deleteStockKlineById(String id);

    /**
     * 批量删除股票K线数据
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockKlineByIds(String[] ids);

    /**
     * 根据股票代码和交易日期判断是否存在记录
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日期
     * @return 是否存在记录
     */
    boolean existsByStockAndDate(@Param("stockCode") String stockCode, @Param("tradeDate") Date tradeDate);

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
    public int insertOrUpdateBatch(@Param("list") List<StockKline> klines);

    List<LocalDate> selectExistsDates(@Param("stockCode") String stockCode, @Param("tradeDateList") List<LocalDate> tradeDateList);

    List<StockKline> queryWeekAllStockKline(@Param("stockCode") String stockCode, @Param("tradeDateList") List<LocalDate> tradeDateList);

    /**
     * Ranking 1: Current Max High vs Last Max High
     */
    List<StockRankingStat> selectHighVsHighRanking();

    /**
     * Ranking 2: Current Min Low vs Last Min Low
     */
    List<StockRankingStat> selectLowVsLowRanking();

    /**
     * Ranking 3: Current Latest Close vs Last Max High
     */
    List<StockRankingStat> selectLatestVsHighRanking();

    /**
     * Ranking 4: Current Latest Close vs Last Min Low
     */
    List<StockRankingStat> selectLatestVsLowRanking();

    /**
     * Ranking 5: Weekly Gain Ranking
     */
    List<StockRankingStat> selectWeeklyGainRanking();

    /**
     * Ranking 6: Weekly Loss Ranking
     */
    List<StockRankingStat> selectWeeklyLossRanking();
}
