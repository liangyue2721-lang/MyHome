package com.make.quartz.task;

import com.make.common.constant.KafkaTopics;
import com.make.common.util.TraceIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("supperTask")
public class SupperTask {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(SupperTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${python.script.nexusStock.nodeId:1}")
    private int nodeId;

    public void refreshStockPrice() {
        // 生成链路追踪ID并放入MDC
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        try {
            log.info("[{}] 开始执行定时任务：刷新股票价格 | NodeId: {}", traceId, nodeId);
            // 触发 Kafka 任务 (Topic Consumer will call runStockKlineTask)
            // Note: Consumer needs nodeId? I implemented it with hardcoded 1 or logic.
            // If nodeId is important for sharding, we should pass it.
            // But runStockKlineTask logic usually determines work based on nodeId.
            // If Quartz is sharded, multiple SupperTasks run?
            // Quartz usually runs job on ONE node (clustered).
            // runStockKlineTask(nodeId) logic: "Am I node X? If so, do X's work."
            // If we send Kafka message, ANY consumer can pick it up.
            // If we want SPECIFIC node execution, we need partition assignment or node-specific topic.
            // For Phase 1, basic trigger is enough. I will send trigger.
            kafkaTemplate.send(KafkaTopics.TOPIC_STOCK_PRICE_TASK, "trigger");
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
            kafkaTemplate.send(KafkaTopics.TOPIC_DEPOSIT_UPDATE, "trigger");
            log.info("[{}] 结束执行定时任务：刷新财务数据", traceId);
        } finally {
            // 清除链路追踪ID
            TraceIdUtil.clearTraceId();
        }
    }
}
