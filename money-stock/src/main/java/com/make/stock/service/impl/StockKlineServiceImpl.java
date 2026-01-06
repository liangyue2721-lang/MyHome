package com.make.stock.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.domain.vo.StockRankingStat;
import com.make.stock.mapper.StockKlineMapper;
import com.make.stock.domain.StockKline;
import com.make.stock.service.IStockKlineService;

/**
 * 股票K线数据Service业务层处理
 *
 * @author erqi
 * @date 2025-11-03
 */
@Service
public class StockKlineServiceImpl implements IStockKlineService {

    @Autowired
    private StockKlineMapper stockKlineMapper;

    /**
     * 查询股票K线数据
     *
     * @param id 股票K线数据主键
     * @return 股票K线数据
     */
    @Override
    public StockKline selectStockKlineById(String id) {
        return stockKlineMapper.selectStockKlineById(id);
    }

    /**
     * 查询股票K线数据列表
     *
     * @param stockKline 股票K线数据
     * @return 股票K线数据
     */
    @Override
    public List<StockKline> selectStockKlineList(StockKline stockKline) {
        return stockKlineMapper.selectStockKlineList(stockKline);
    }

    /**
     * 新增股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    @Override
    public int insertStockKline(StockKline stockKline) {
        stockKline.setCreateTime(DateUtils.getNowDate());
        return stockKlineMapper.insertStockKline(stockKline);
    }

    /**
     * 修改股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    @Override
    public int updateStockKline(StockKline stockKline) {
        stockKline.setUpdateTime(DateUtils.getNowDate());
        return stockKlineMapper.updateStockKline(stockKline);
    }

    /**
     * 批量删除股票K线数据
     *
     * @param ids 需要删除的股票K线数据主键
     * @return 结果
     */
    @Override
    public int deleteStockKlineByIds(String[] ids) {
        return stockKlineMapper.deleteStockKlineByIds(ids);
    }

    /**
     * 删除股票K线数据信息
     *
     * @param id 股票K线数据主键
     * @return 结果
     */
    @Override
    public int deleteStockKlineById(String id) {
        return stockKlineMapper.deleteStockKlineById(id);
    }

    /**
     * 根据股票代码和交易日期判断是否存在记录
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日期
     * @return 是否存在记录
     */
    @Override
    public boolean existsByStockAndDate(String stockCode, Date tradeDate) {
        return stockKlineMapper.existsByStockAndDate(stockCode, tradeDate);
    }


    /**
     * 根据股票代码和交易日期更新股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    @Override
    public int updateByStockCodeAndTradeDate(StockKline stockKline) {
        stockKline.setUpdateTime(DateUtils.getNowDate());
        return stockKlineMapper.updateByStockCodeAndTradeDate(stockKline);
    }

    /**
     * 批量插入或更新股票K线数据
     *
     * @param klines 股票K线数据列表
     * @return 影响的行数
     */
    @Override
    public int insertOrUpdateBatch(List<StockKline> klines) {
        return stockKlineMapper.insertOrUpdateBatch(klines);
    }

    @Override
    public void batchUpdateByStockCodeAndTradeDate(List<StockKline> updateList) {
        stockKlineMapper.insertOrUpdateBatch(updateList);
    }

    @Override
    public List<LocalDate> selectExistsDates(String stockCode, List<LocalDate> tradeDateList) {
        return stockKlineMapper.selectExistsDates(stockCode, tradeDateList);
    }

    @Override
    public List<StockKline> queryWeekAllStockKline(String stockCode, List<LocalDate> tradeDateList) {
        return stockKlineMapper.queryWeekAllStockKline(stockCode, tradeDateList);
    }

    @Override
    public List<StockRankingStat> selectStockRanking(String type) {
        switch (type) {
            case "HIGH_VS_HIGH":
                return stockKlineMapper.selectHighVsHighRanking();
            case "LOW_VS_LOW":
                return stockKlineMapper.selectLowVsLowRanking();
            case "LATEST_VS_HIGH":
                return stockKlineMapper.selectLatestVsHighRanking();
            default:
                return Collections.emptyList();
        }
    }
}
