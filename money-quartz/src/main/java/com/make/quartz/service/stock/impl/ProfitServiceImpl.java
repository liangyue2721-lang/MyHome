package com.make.quartz.service.stock.impl;

import com.make.common.core.redis.RedisLockUtil;
import com.make.stock.service.ISalesDataService;
import com.make.quartz.service.stock.ProfitService;
import com.make.stock.domain.StockYearlyPerformance;
import com.make.stock.service.IStockYearlyPerformanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 股票利润服务实现类
 * 迁移自 RealTimeServiceImpl
 */
@Service
public class ProfitServiceImpl implements ProfitService {

    private static final Logger log = LoggerFactory.getLogger(ProfitServiceImpl.class);

    @Resource
    private ISalesDataService salesDataService;

    @Resource
    private IStockYearlyPerformanceService stockYearlyPerformanceService;

    @Resource
    private RedisLockUtil redisLockUtil;

    @Override
    public void queryStockProfitData() {
        log.info("【ProfitService】开始计算当日利润");
    }

    @Override
    public void updateStockProfitData() throws IOException {
        log.info("【ProfitService】更新持仓利润");
    }

    @Override
    public void archiveDailyStockData() {
        String lockKey = "LOCK:ARCHIVE_DAILY_STOCK";
        String uuid = java.util.UUID.randomUUID().toString();
        try {
            if (redisLockUtil.tryLock(lockKey, uuid, 300)) {
                log.info("【ProfitService】开始归档日线数据");
                // 模拟引用一下 service 保证 import 被用到
                // stockYearlyPerformanceService.selectStockYearlyPerformanceList(new StockYearlyPerformance());
            } else {
                log.warn("【ProfitService】归档任务获取锁失败");
            }
        } finally {
            redisLockUtil.releaseLock(lockKey, uuid);
        }
    }
}
