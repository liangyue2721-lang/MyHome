package com.make.quartz.service.stock.impl;

import com.make.quartz.service.impl.StockETFrocessor;
import com.make.quartz.service.stock.EtfTaskService;
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
