package com.make.stock.service.scheduled.impl;

import com.alibaba.fastjson2.JSON;
import com.make.common.constant.KafkaTopics;
import com.make.common.core.NodeRegistry;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.StockKlineTask;
import com.make.stock.domain.dto.ProcessResult;
import com.make.stock.service.IStockKlineTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 股票K线任务执行器
 * <p>
 * 重构说明：
 * 采用 Kafka 生产-消费模型，实现动态负载均衡。
 * 1. SmartLifecycle & Watchdog: 实现自驱动，每5分钟Master节点自动扫描并提交任务。
 * 2. submitTasks: 生产者，查询所有任务并生产消息到 Kafka。
 * 3. processSingleTask: 消费者，监听 Topic 处理单个 K线任务。
 * </p>
 */
@Component
public class StockKlineTaskExecutor implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StockKlineTaskExecutor.class);

    @Resource
    private IStockKlineTaskService stockKlineTaskService;

    @Resource
    private StockKlineProcessor stockKlineProcessor;

    @Resource
    private StockKlineRepositoryService repositoryService;

    @Resource
    private WatchStockUpdater watchStockUpdater;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    private NodeRegistry nodeRegistry;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService watchdogExecutor;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("StockKlineTaskExecutor started. Starting Watchdog...");
            watchdogExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "KlineWatchDog");
                t.setDaemon(true);
                return t;
            });
            // Run Watchdog every 5 minutes
            watchdogExecutor.scheduleWithFixedDelay(this::runWatchdog, 1, 5, TimeUnit.MINUTES);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (watchdogExecutor != null) {
            watchdogExecutor.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }

    /**
     * 看门狗 (Master Only)
     * 定期扫描并提交所有 K线任务
     */
    public void runWatchdog() {
        if (!running.get()) return;

        // Master Only
        if (!nodeRegistry.isMaster()) {
            return;
        }

        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            log.info("[Kline-Watchdog] 触发自动扫描 TraceId={}", traceId);
            submitTasks(traceId);
        } catch (Exception e) {
            log.error("[Kline-Watchdog] 扫描异常", e);
        }
    }

    /**
     * 提交所有 K线任务到 Kafka (Producer)
     * <p>
     * 逻辑说明：
     * 1. 忽略 NodeId，直接获取所有待处理的 K线任务 (stockKlineTaskService.getStockAllTask)。
     * 2. 遍历任务列表，将每个任务封装成消息发送到 Kafka (TOPIC_KLINE_TASK)。
     * 3. 消费者端将接收并处理这些任务，从而实现集群间的负载均衡。
     *
     * @param traceId 链路追踪ID
     */
    public void submitTasks(String traceId) {
        log.info("=====【Kline 任务提交】TraceId={} =====", traceId);

        // 1. 获取所有任务 (Mapper层已忽略 NodeId，传 0 即可)
        List<StockKlineTask> taskList = stockKlineTaskService.getStockAllTask(0);
        if (taskList == null || taskList.isEmpty()) {
            log.info("❌ [Kline] TraceId={} 无任务", traceId);
            return;
        }

        log.info("🏁 [Kline-Producer] TraceId={} 待提交任务数={}", traceId, taskList.size());

        // 2. 逐个发送到 Kafka
        for (StockKlineTask task : taskList) {
            try {
                // 构建消息包装类，带上 TraceId
                TaskWrapper wrapper = new TaskWrapper(traceId, task);
                String json = JSON.toJSONString(wrapper);

                // 使用 stockCode 作为 Key
                kafkaTemplate.send(KafkaTopics.TOPIC_KLINE_TASK, task.getStockCode(), json);
            } catch (Exception e) {
                log.error("❌ [Kline-Producer] 发送失败 TraceId={} Code={} Err={}", traceId, task.getStockCode(), e.getMessage());
            }
        }

        log.info("=====【Kline 任务提交完成】TraceId={} =====", traceId);
    }

    /**
     * 任务消息包装类
     */
    public static class TaskWrapper {
        public String traceId;
        public StockKlineTask task;

        public TaskWrapper() {}
        public TaskWrapper(String traceId, StockKlineTask task) {
            this.traceId = traceId;
            this.task = task;
        }
    }

    /**
     * 处理单个 K线任务 (Consumer)
     * <p>
     * 逻辑说明：
     * 1. 解析 Kafka 消息。
     * 2. 调用 StockKlineProcessor 处理 K线数据。
     * 3. 根据处理结果 (ProcessResult)，执行数据库插入/更新操作。
     * 4. 更新 WatchStock 监控信息。
     * 5. 标记任务完成 (batchFinishTask)。
     *
     * @param message Kafka 消息内容
     */
    public void processSingleTask(String message) {
        try {
            // 1. 解析消息
            TaskWrapper wrapper = JSON.parseObject(message, TaskWrapper.class);
            if (wrapper == null || wrapper.task == null) return;

            String traceId = wrapper.traceId;
            StockKlineTask task = wrapper.task;

            // 2. 处理数据 (复用原有 Processor 逻辑)
            ProcessResult r = stockKlineProcessor.processTaskData(task, DF);

            // 3. 处理持久化
            if (r.success) {
                // 批量插入 (虽为单任务，ProcessResult 仍返回 List)
                if (!r.insertList.isEmpty()) {
                    repositoryService.insertOrUpdateBatch(r.insertList);
                }
                // 批量更新
                if (!r.updateList.isEmpty()) {
                    repositoryService.batchUpdateByStockCodeAndTradeDate(r.updateList);
                    // 更新监控信息
                    for (StockKline k : r.updateList) {
                         watchStockUpdater.processWatchStock(k);
                    }
                }

                // 标记任务完成
                stockKlineTaskService.batchFinishTask(Collections.singletonList(task.getId()));

                log.debug("✅ [Kline-Consumer] 成功 TraceId={} Code={}", traceId, task.getStockCode());
            } else {
                log.error("❌ [Kline-Consumer] 业务处理失败 TraceId={} Code={}", traceId, task.getStockCode());
            }

        } catch (Exception e) {
            log.error("❌ [Kline-Consumer] 系统异常 Msg={} Err={}", message, e);
        }
    }
}
