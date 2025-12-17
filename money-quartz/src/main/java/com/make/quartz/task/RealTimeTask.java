package com.make.quartz.task;

import com.make.quartz.service.IRealTimeService;
import com.make.quartz.service.IRealTimeStockService;
import com.make.quartz.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;


/**
 * 获取实时数据的定时任务调度测试
 *
 * @author ruoyi
 */
@Component("realTimeTask")
public class RealTimeTask {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(RealTimeTask.class);

    @Resource
    private IRealTimeStockService realTimeStockService;

    @Resource
    private IRealTimeService realTimeService;


    /**
     * 更新Redis数据存储
     * <p>
     * 注释原因：逻辑已迁移至 IStockTaskServiceImpl 及相关处理器，统一由 Quartz 调度。
     * 注释时间：2023-12-17
     */
//    public void updateInMemoryData() {
//        long startTime = System.currentTimeMillis();
//        if (!DateUtil.isValidWorkday()) {
//            return;
//        }
//        try {
//            realTimeStockService.refreshInMemoryMapEntries();
//            realTimeStockService.batchSyncStockDataToDB2();
//            realTimeStockService.refreshWealthInMemoryMapEntries();
////            realTimeService.wealthDBDataBak();
//            realTimeService.updateEtfData();
//            queryListingStatusColumn();
//        } catch (Exception e) {
//            log.error("执行realTimeTask更新内存中股票数据任务失败:", e);
//        }
//        long endTime = System.currentTimeMillis();
//        log.info("执行realTimeTask更新内存中股票数据任务完成,耗时{}ms", endTime - startTime);
//
//    }


    /**
     * 更新美股实时行情数据。
     */
    public void updateWatchStockUs() {
        long startTime = System.currentTimeMillis();
        try {
            realTimeService.updateWatchStockUs();
        } catch (Exception e) {
            log.error("执行realTimeTask更新美股实时行情数据。任务失败:", e);
        }
        long endTime = System.currentTimeMillis();
        log.info("执行realTimeTask更新美股实时行情数据。任务任务完成,耗时{}ms", endTime - startTime);

    }

    /**
     * 查询今天是否有上市的股票
     * 通过IP白名单机制保障数据源合法性
     */
    private void queryListingStatusColumn() {
        long startTime = System.currentTimeMillis();
        if (!DateUtil.isValidWorkday()) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date midnight = calendar.getTime();
        log.info("查询今天是否有上市的股票开始时间{}", midnight);
        try {
            realTimeService.queryListingStatusColumn(midnight);
        } catch (Exception e) {
            log.error("查询今天是否有上市的股票中断", e);
        }
        long endTime = System.currentTimeMillis();
        log.info("查询今天{}是否有上市的股票任务结束，耗时：{}ms", midnight, endTime - startTime);

    }
}
