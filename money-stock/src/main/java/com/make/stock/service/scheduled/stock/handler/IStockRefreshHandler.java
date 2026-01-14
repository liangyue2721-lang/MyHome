package com.make.stock.service.scheduled.stock.handler;

import com.make.stock.domain.StockRefreshTask;

/**
 * 股票刷新业务处理器接口
 * 负责执行具体的股票刷新业务逻辑：获取数据、更新DB、计算利润、通知等
 */
public interface IStockRefreshHandler {

    /**
     * 处理股票刷新任务
     *
     * @param task 任务信息
     */
    void refreshStock(StockRefreshTask task);
}
