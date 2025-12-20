package com.make.quartz.service.stock;

/**
 * K线聚合服务接口
 * 负责调度和聚合 K 线任务
 */
public interface KlineAggregatorService {

    /**
     * 运行股票K线任务（包含自选、ETF、历史K线）
     * @param nodeId 节点ID
     */
    void runStockKlineTask(int nodeId);
}
