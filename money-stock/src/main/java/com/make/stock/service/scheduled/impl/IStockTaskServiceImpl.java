package com.make.stock.service.scheduled.impl;

import com.make.stock.service.scheduled.IStockTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 股票K线任务入口服务实现类 (Facade)
 *
 * 逻辑已迁移至各个 Processor 的 Watchdog (SmartLifecycle)
 */
@Service
public class IStockTaskServiceImpl implements IStockTaskService {

    private static final Logger log = LoggerFactory.getLogger(IStockTaskServiceImpl.class);

    @Resource
    private StockWatchProcessor stockWatchProcessor;

    @Resource
    private StockETFProcessor stockETFProcessor;

    @Resource
    private StockKlineTaskExecutor stockKlineTaskExecutor;

    // 保留构造函数以兼容旧代码（虽然现在推荐使用 @Resource）
    public IStockTaskServiceImpl() {}

    @Override
    public void runStockKlineTask(int nodeId) {
        log.info("【Facade】IStockTaskService 手动触发 Watchdog (Master Only Check inside)");

        // 尝试手动触发 Watchdog (仅用于调试或手动调用)
        // 实际上任务现在由 SmartLifecycle 自动驱动

        try {
            stockWatchProcessor.runWatchdog();
            stockETFProcessor.runWatchdog();
            stockKlineTaskExecutor.runWatchdog();
        } catch (Exception e) {
            log.error("手动触发 Watchdog 失败", e);
        }
    }
}
