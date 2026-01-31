package com.make.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 获取实时数据的定时任务调度测试
 * (Restored as Facade for backward compatibility with existing sys_job entries)
 *
 * @author ruoyi
 */
@Component("realTimeTask")
public class RealTimeTask {

    private static final Logger log = LoggerFactory.getLogger(RealTimeTask.class);

    @Resource
    private StockTriggerTask stockTriggerTask;

    /**
     * 对应 Job 28: realTimeTask.updateWatchStockUs
     */
    public void updateWatchStockUs() {
        log.info("【RealTimeTask】Delegating updateWatchStockUs to StockTriggerTask");
        stockTriggerTask.triggerWatchStockUs();
    }

    /**
     * 对应 Job?: realTimeTask.queryListingStatusColumn
     * (Although not explicitly in the provided list as active, it was in the commented code)
     */
    public void queryListingStatusColumn() {
        log.info("【RealTimeTask】Delegating queryListingStatusColumn to StockTriggerTask");
        stockTriggerTask.triggerQueryListing();
    }

    /**
     * 对应 Job 32: realTimeTask.updateInMemoryData
     * (Logic missing in new system, logging warning)
     */
    public void updateInMemoryData() {
        log.warn("【RealTimeTask】updateInMemoryData called but logic is deprecated/missing.");
    }
}
