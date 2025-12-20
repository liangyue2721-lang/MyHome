package com.make.quartz.service.stock.impl;

import com.make.quartz.service.impl.StockWatchProcessor;
import com.make.quartz.service.stock.WatchTaskService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class WatchTaskServiceImpl implements WatchTaskService {

    @Resource
    private StockWatchProcessor stockWatchProcessor;

    @Override
    public void executeWatchTask(String traceId) {
        stockWatchProcessor.processTask(traceId);
    }
}
