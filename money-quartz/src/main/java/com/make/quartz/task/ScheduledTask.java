package com.make.quartz.task;

import com.make.common.core.redis.RedisCache;
import com.make.common.util.TraceIdUtil;
import com.make.quartz.service.IRealTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTask.class);

    @Resource
    private IRealTimeService realTimeService;

    @Resource
    private RedisCache redisCache;

    @Value("${python.script.nexusStock.node-id:1}")
    private int nodeId;

    /**
     * 标志位：控制当次执行不重复
     */
    private final AtomicBoolean stockPriceUpdateRunning = new AtomicBoolean(false);
    private final AtomicBoolean highPriceRateRunning = new AtomicBoolean(false);

    private static final long REDIS_LOCK_EXPIRE_SECONDS = 60;

    /**
     * 定时任务：每个交易日凌晨 00:00 更新股票最高价涨跌幅
     * 仅在 Mon~Fri 执行
     */
    @Scheduled(cron = "0 0 0 * * MON-FRI")
    public void updateStockHighPriceRates() {
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);

        try {
            log.info("[{}] 开始执行 股票最高价涨跌幅 更新任务", traceId);

            // 本地并发控制
            if (!highPriceRateRunning.compareAndSet(false, true)) {
                log.warn("[{}] 上次任务尚未结束，跳过本次执行", traceId);
                return;
            }

            long start = System.currentTimeMillis();
            realTimeService.updateWatchStockYearLow();
            long end = System.currentTimeMillis();

            log.info("[{}] 股票最高价涨跌幅 更新完成, 耗时 {} ms", traceId, (end - start));


        } catch (Exception e) {
            log.error("[{}] 股票最高价涨跌幅 更新任务失败", traceId, e);
        } finally {
            highPriceRateRunning.set(false);
            TraceIdUtil.clearTraceId();
        }
    }

    /**
     * 定时任务：每 5 秒执行一次 实时股票价格更新
     */
    @Scheduled(cron = "*/5 * * * * ?")
    public void updateStockPriceTaskRunning() {
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);

        try {
            log.info("[{}] 开始执行 实时股票价格 更新任务 nodeId={}", traceId, nodeId);

            if (!stockPriceUpdateRunning.compareAndSet(false, true)) {
                log.warn("[{}] 上一次股票价格更新未结束，跳过执行", traceId);
                return;
            }

            long start = System.currentTimeMillis();
            realTimeService.updateStockPriceTaskRunning(nodeId);
            long end = System.currentTimeMillis();

            log.info("[{}] 实时股票价格 更新完成, 耗时 {} ms", traceId, (end - start));
        } catch (Exception e) {
            log.error("[{}] 实时股票价格 更新任务异常", traceId, e);
        } finally {
            stockPriceUpdateRunning.set(false);
            TraceIdUtil.clearTraceId();
        }
    }

    /**
     * 定时任务：每 10 分钟记录线程池状态
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void logAllThreadPoolStatus() {
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);

        log.info("[{}] 开始记录线程池状态", traceId);
        long start = System.currentTimeMillis();

        try {
            realTimeService.logAllThreadPoolStatus();
        } catch (Exception e) {
            log.error("[{}] 记录线程池状态异常", traceId, e);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("[{}] 线程池状态记录完成, 耗时 {} ms", traceId, duration);

        TraceIdUtil.clearTraceId();
    }
}
