package com.make.quartz.service.stock;

/**
 * 股票利润服务接口
 * 负责计算收益、归档日线数据
 */
public interface ProfitService {

    /**
     * 查询当日所有交易记录并更新用户净利润
     */
    void queryStockProfitData();

    /**
     * 更新股票交易利润数据
     */
    void updateStockProfitData() throws java.io.IOException;

    /**
     * 每日股票数据归档任务
     */
    void archiveDailyStockData();
}
