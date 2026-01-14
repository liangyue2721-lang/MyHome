package com.make.stock.service.scheduled.stock;

/**
 * 关注股票服务接口
 * 负责自选股监控、年高年低更新、美股更新
 */
public interface WatchService {

    /**
     * 更新关注股票的利润数据
     */
    void updateWatchStockProfitData();

    /**
     * 更新关注股票的周低、周高、年低、年高价格
     */
    void updateWatchStockYearLow();

    /**
     * 更新关注的美股行情数据
     */
    void updateWatchStockUs();
}
