package com.make.stock.service.scheduled.stock.impl;

import com.make.stock.service.scheduled.impl.StockETFProcessor;
import com.make.stock.service.scheduled.stock.EtfTaskService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class EtfTaskServiceImpl implements EtfTaskService {

    @Resource
    private StockETFProcessor stockETFProcessor;

    @Override
    public void executeEtfTask(String traceId) {
        stockETFProcessor.submitTasks(traceId);
    }
}
