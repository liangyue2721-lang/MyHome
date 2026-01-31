package com.make.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * SupperTask
 * (Restored as Facade for backward compatibility with existing sys_job entries)
 */
@Component("supperTask")
public class SupperTask {

    private static final Logger log = LoggerFactory.getLogger(SupperTask.class);

    @Resource
    private StockTriggerTask stockTriggerTask;

    @Resource
    private FinanceTriggerTask financeTriggerTask;

    /**
     * 对应 Job 30/33: supperTask.refreshStockPrice
     */
    public void refreshStockPrice() {
        log.info("【SupperTask】Delegating refreshStockPrice to StockTriggerTask");
        stockTriggerTask.triggerStockPriceTask();
    }

    /**
     * 对应 Job 31/34: supperTask.refreshFinanceData
     */
    public void refreshFinanceData() {
        log.info("【SupperTask】Delegating refreshFinanceData to FinanceTriggerTask");
        financeTriggerTask.triggerDepositUpdate();
    }
}
