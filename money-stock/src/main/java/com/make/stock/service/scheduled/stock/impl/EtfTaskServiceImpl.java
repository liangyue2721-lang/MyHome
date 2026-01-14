package com.make.stock.service.scheduled.stock.impl;

import com.make.stock.service.scheduled.impl.StockETFrocessor;
import com.make.stock.service.scheduled.stock.EtfTaskService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class EtfTaskServiceImpl implements EtfTaskService {

    @Resource
    private StockETFrocessor stockETFrocessor;

    @Override
    public void executeEtfTask(String traceId) {
        stockETFrocessor.processTask(traceId);
    }
}
