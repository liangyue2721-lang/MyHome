package com.make.stock.service.scheduled.stock.impl;

import com.make.stock.service.scheduled.impl.StockWatchProcessor;
import com.make.stock.service.scheduled.stock.WatchTaskService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class WatchTaskServiceImpl implements WatchTaskService {

    @Resource
    private StockWatchProcessor stockWatchProcessor;

    @Override
    public void executeWatchTask(String traceId) {
        // processTask is deprecated. Use Watchdog to ensure loops are running.
        stockWatchProcessor.runWatchdog();
    }
}
