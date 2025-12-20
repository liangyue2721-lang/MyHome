package com.make.quartz.service.impl;

import com.make.quartz.service.IStockTaskService;
import com.make.quartz.service.stock.KlineAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 股票K线任务入口服务实现类 (Facade)
 *
 * 逻辑已迁移至 com.make.quartz.service.stock.KlineAggregatorService
 */
@Service
public class IStockTaskServiceImpl implements IStockTaskService {

    private static final Logger log = LoggerFactory.getLogger(IStockTaskServiceImpl.class);

    @Resource
    private KlineAggregatorService klineAggregatorService;

    // 保留构造函数以兼容旧代码（虽然现在推荐使用 @Resource）
    // 如果有旧的 xml 配置依赖此构造函数，则需要保留
    public IStockTaskServiceImpl() {}

    @Override
    public void runStockKlineTask(int nodeId) {
        log.info("【Facade】IStockTaskService 委托调用 KlineAggregatorService.runStockKlineTask");
        klineAggregatorService.runStockKlineTask(nodeId);
    }
}
