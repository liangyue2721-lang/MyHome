package com.make.quartz.task;

/*
import com.make.common.constant.KafkaTopics;
import com.make.common.util.TraceIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
*/

// @Component("supperTask")
public class SupperTask {

    /*
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(SupperTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${python.script.nexusStock.nodeId:1}")
    private int nodeId;

    public void refreshStockPrice() {
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        try {
            log.info("[{}] 开始执行定时任务：刷新股票价格 | NodeId: {}", traceId, nodeId);
            kafkaTemplate.send(KafkaTopics.TOPIC_STOCK_PRICE_TASK, "trigger");
            log.info("[{}] 结束执行定时任务：刷新股票价格", traceId);
        } catch (Exception e) {
            log.error("[{}] 刷新股票价格任务执行异常", traceId, e);
        } finally {
            TraceIdUtil.clearTraceId();
        }
    }

    public void refreshFinanceData() {
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        try {
            log.info("[{}] 开始执行定时任务：刷新财务数据", traceId);
            kafkaTemplate.send(KafkaTopics.TOPIC_DEPOSIT_UPDATE, "trigger");
            log.info("[{}] 结束执行定时任务：刷新财务数据", traceId);
        } finally {
            TraceIdUtil.clearTraceId();
        }
    }
    */
}
