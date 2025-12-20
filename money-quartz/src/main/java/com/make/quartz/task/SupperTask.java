package com.make.quartz.task;

import com.make.common.util.TraceIdUtil;
import com.make.quartz.service.IFinanceTaskService;
import com.make.quartz.service.IStockTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("supperTask")
public class SupperTask {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(SupperTask.class);

    @Resource
    private IStockTaskService stockTaskService;

    @Resource
    private IFinanceTaskService financeTaskService;

    @Value("${python.script.nexusStock.nodeId:1}")
    private int nodeId;

    public void refreshStockPrice() {
        // 生成链路追踪ID并放入MDC
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        try {
            log.info("[{}] 开始执行定时任务：刷新股票价格 | NodeId: {}", traceId, nodeId);
            // 确保nodeId有效
            if (nodeId <= 0) {
                log.warn("[{}] NodeId {} 无效，使用默认值 1", traceId, nodeId);
                nodeId = 1;
            }
            stockTaskService.runStockKlineTask(nodeId);
            log.info("[{}] 结束执行定时任务：刷新股票价格", traceId);
        } catch (Exception e) {
            log.error("[{}] 刷新股票价格任务执行异常", traceId, e);
        } finally {
            // 清除链路追踪ID
            TraceIdUtil.clearTraceId();
        }
    }

    public void refreshFinanceData() {
        // 生成链路追踪ID并放入MDC
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        try {
            log.info("[{}] 开始执行定时任务：刷新财务数据", traceId);
            financeTaskService.refreshDepositAmount();
            log.info("[{}] 结束执行定时任务：刷新财务数据", traceId);
        } finally {
            // 清除链路追踪ID
            TraceIdUtil.clearTraceId();
        }
    }
}
