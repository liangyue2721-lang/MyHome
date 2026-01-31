package com.make.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 获取固定时间的定时任务调度测试
 * (Restored as Facade for backward compatibility with existing sys_job entries)
 *
 * @author ruoyi
 */
@Component("fixedTimeTask")
public class FixedTimeTask {

    private static final Logger log = LoggerFactory.getLogger(FixedTimeTask.class);

    @Resource
    private StockTriggerTask stockTriggerTask;

    @Resource
    private FinanceTriggerTask financeTriggerTask;

    /**
     * 对应 Job 7: fixedTimeTask.refreshNewStockInformation
     */
    public void refreshNewStockInformation() {
        log.info("【FixedTimeTask】Delegating refreshNewStockInformation to StockTriggerTask");
        stockTriggerTask.triggerNewStockInfo();
    }

    /**
     * 对应 Job 19: fixedTimeTask.updateDepositAmount
     * 刷新存款金额（年度汇总）
     */
    public void updateDepositAmount() {
        log.info("【FixedTimeTask】Delegating updateDepositAmount to FinanceTriggerTask");
        financeTriggerTask.triggerDepositUpdate();
    }

    /**
     * 对应 Job 20: fixedTimeTask.updateICBCDepositAmount
     * 每月更新月供资产
     */
    public void updateICBCDepositAmount() {
        log.info("【FixedTimeTask】Delegating updateICBCDepositAmount to FinanceTriggerTask");
        financeTriggerTask.triggerIcbcDepositUpdate();
    }
}
