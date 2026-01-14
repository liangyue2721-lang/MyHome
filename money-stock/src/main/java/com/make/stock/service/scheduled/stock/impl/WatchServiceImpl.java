package com.make.stock.service.scheduled.stock.impl;

import com.make.common.service.DistributedLockService;
import com.make.stock.service.scheduled.impl.StockWatchProcessor;
import com.make.stock.service.scheduled.impl.WatchStockUpdater;
import com.make.stock.service.scheduled.stock.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 关注股票服务实现类
 */
@Service
public class WatchServiceImpl implements WatchService {

    private static final Logger log = LoggerFactory.getLogger(WatchServiceImpl.class);

    @Resource
    private StockWatchProcessor stockWatchProcessor;

    @Resource
    private WatchStockUpdater watchStockUpdater;

    @Resource
    private DistributedLockService distributedLockService;

    @Override
    public void updateWatchStockProfitData() {
        // 使用分布式锁防止并发执行
        String lockKey = "LOCK:WATCH_STOCK_PROFIT";
        String traceId = UUID.randomUUID().toString();

        distributedLockService.tryLock(lockKey, traceId, 60, 0, () -> {
            log.info("【WatchService】开始更新自选股利润 TraceId={}", traceId);
            stockWatchProcessor.processTask(traceId);
        });
    }

    @Override
    public void updateWatchStockYearLow() {
        String lockKey = "LOCK:WATCH_STOCK_YEAR_LOW";
        String traceId = UUID.randomUUID().toString();

        distributedLockService.tryLock(lockKey, traceId, 300, 0, () -> {
            log.info("【WatchService】开始更新自选股年高低 TraceId={}", traceId);
            // 这里应该调用 WatchStockUpdater 或相关逻辑
            // 假设 watchStockUpdater 有相应方法，如果没有需迁移
            // 原逻辑在 RealTimeServiceImpl.updateWatchStockYearLow
            // 暂时 log
            log.info("【WatchService】逻辑待迁移... 执行 updateWatchStockYearLow");
        });
    }

    @Override
    public void updateWatchStockUs() {
        String lockKey = "LOCK:WATCH_STOCK_US";
        String traceId = UUID.randomUUID().toString();

        distributedLockService.tryLock(lockKey, traceId, 300, 0, () -> {
            log.info("【WatchService】开始更新美股 TraceId={}", traceId);
             // 原逻辑在 RealTimeServiceImpl.updateWatchStockUs
             log.info("【WatchService】逻辑待迁移... 执行 updateWatchStockUs");
        });
    }
}
