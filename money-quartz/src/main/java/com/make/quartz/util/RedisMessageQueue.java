package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.domain.SysJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * Redis 队列调度与消费中心（Redis-only 引擎）
 *
 * <p>实现目标：
 * 1) 任务统一进入 Redis 队列（不再依赖 Quartz Trigger / @Scheduled 生产）
 * 2) 队列内部嵌入 Scheduled 时间检查：使用 Delay ZSET（score=scheduledAtMillis）
 * 3) 多实例：每个节点监听自己的 ready 队列；delay 推进由每个实例都可做（使用简单竞争即可）
 *
 * <p>结构：
 * - delay zset：TASK_DELAY_ZSET_PREFIX + nodeId
 * - ready list：TASK_QUEUE_PREFIX + nodeId + (HIGH/NORMAL)
 * - processing list：TASK_QUEUE_PREFIX + nodeId + PROCESSING（用于防丢与回补）
 */
@Component
public class RedisMessageQueue implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueue.class);

    private static volatile RedisMessageQueue instance;

    private final RedisTemplate<String, String> redisTemplate;
    private final QuartzProperties quartzProperties;

    /**
     * 每节点 ready 队列前缀
     */
    private static final String TASK_QUEUE_PREFIX = "mq:task:";
    private static final String HIGH_PRIORITY_SUFFIX = ":high";
    private static final String NORMAL_PRIORITY_SUFFIX = ":normal";
    private static final String PROCESSING_QUEUE_SUFFIX = ":processing";

    /**
     * 每节点 delay zset 前缀
     */
    private static final String TASK_DELAY_ZSET_PREFIX = "mq:delay:";

    /**
     * 是否运行（生命周期）
     */
    private volatile boolean running = false;

    /**
     * 当前节点（由 TaskExecutionService 传入）
     */
    private volatile String currentNodeId;

    /**
     * 监听线程池：阻塞监听 ready 队列
     */
    private ThreadPoolExecutor listenerExecutor;

    /**
     * 消费执行线程池：真正执行任务
     */
    private ThreadPoolExecutor consumerExecutor;

    /**
     * 内部调度器：推进 delay -> ready、回补 processing
     */
    private ScheduledExecutorService internalScheduler;

    public RedisMessageQueue(RedisTemplate<String, String> redisTemplate, QuartzProperties quartzProperties) {
        this.redisTemplate = redisTemplate;
        this.quartzProperties = quartzProperties;
    }

    /**
     * 单例获取（兼容你工程里 existing 用法）
     */
    public static RedisMessageQueue getInstance() {
        if (instance == null) {
            instance = SpringUtils.getBean(RedisMessageQueue.class);
        }
        return instance;
    }

    /**
     * 初始化线程池与内部调度器
     *
     * <p>注意：队列“是否开始消费”由 startListening 控制（会设置 running=true）。
     */
    @PostConstruct
    public void init() {
        int cpu = Runtime.getRuntime().availableProcessors();

        this.listenerExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                Math.max(2, cpu * quartzProperties.getListenerCoreThreadsMultiple()),
                Math.max(4, cpu * quartzProperties.getListenerMaxThreadsMultiple()),
                200,
                "redis-queue-listener"
        );

        this.consumerExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                quartzProperties.getConsumerCoreSize(),
                quartzProperties.getConsumerMaxSize(),
                quartzProperties.getConsumerQueueCapacity(),
                "redis-queue-consumer"
        );

        this.internalScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "redis-queue-internal-scheduler");
            t.setDaemon(true);
            return t;
        });

        // 启动定时任务：推进 Delayed 消息
        this.internalScheduler.scheduleWithFixedDelay(() -> {
            if (running && currentNodeId != null) {
                try {
                    String highReady = TASK_QUEUE_PREFIX + currentNodeId + HIGH_PRIORITY_SUFFIX;
                    String normalReady = TASK_QUEUE_PREFIX + currentNodeId + NORMAL_PRIORITY_SUFFIX;
                    promoteDueDelayedMessages(currentNodeId, highReady, normalReady);
                } catch (Exception e) {
                    log.warn("[RMQ_PROMOTE_ERR] {}", e.getMessage());
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        // 启动定时任务：回补 Processing 超时消息
        this.internalScheduler.scheduleWithFixedDelay(() -> {
            if (running && currentNodeId != null) {
                try {
                    String processing = TASK_QUEUE_PREFIX + currentNodeId + PROCESSING_QUEUE_SUFFIX;
                    String highReady = TASK_QUEUE_PREFIX + currentNodeId + HIGH_PRIORITY_SUFFIX;
                    String normalReady = TASK_QUEUE_PREFIX + currentNodeId + NORMAL_PRIORITY_SUFFIX;
                    reclaimProcessingTimeout(currentNodeId, processing, highReady, normalReady);
                } catch (Exception e) {
                    log.warn("[RMQ_RECLAIM_ERR] {}", e.getMessage());
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        log.info("[RMQ_INIT] RedisMessageQueue init done. listenerCore={}, consumerCore={}",
                listenerExecutor.getCorePoolSize(), consumerExecutor.getCorePoolSize());
    }

    /**
     * 启动对某个节点队列的监听（消费入口）
     *
     * @param nodeId  节点ID
     * @param handler 消息处理器
     */
    public void startListening(String nodeId, MessageHandler handler) {

        // ===== 关键修复点：创建 final 快照 =====
        final String fixedNodeId = nodeId;
        final String highReady = TASK_QUEUE_PREFIX + fixedNodeId + HIGH_PRIORITY_SUFFIX;
        final String normalReady = TASK_QUEUE_PREFIX + fixedNodeId + NORMAL_PRIORITY_SUFFIX;
        final String processing = TASK_QUEUE_PREFIX + fixedNodeId + PROCESSING_QUEUE_SUFFIX;

        this.currentNodeId = fixedNodeId;
        this.running = true;

        listenerExecutor.submit(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    String msgJson = redisTemplate.opsForList()
                            .rightPopAndLeftPush(highReady, processing, 2, TimeUnit.SECONDS);

                    if (msgJson == null) {
                        msgJson = redisTemplate.opsForList()
                                .rightPopAndLeftPush(normalReady, processing, 2, TimeUnit.SECONDS);
                    }

                    if (msgJson == null) {
                        continue;
                    }


                    final String finalMsgJson = msgJson;

                    TaskMessage msg = JSON.parseObject(finalMsgJson, TaskMessage.class);
                    msg.setOriginalJson(finalMsgJson);

                    // 提交到消费线程池
                    consumerExecutor.execute(() -> {
                        try {
                            handler.handle(msg);
                            acknowledge(nodeId, processing, finalMsgJson);
                        } catch (Exception e) {
                            requeue(nodeId, processing, msg, finalMsgJson, e);
                        }
                    });


                } catch (Exception e) {
                    log.warn("[RMQ_LISTEN_ERR] nodeId={}", fixedNodeId, e);
                }
            }
        });
    }


    /**
     * 停止监听（停止消费）
     */
    public void stopListening() {
        this.running = false;
    }

    /**
     * ACK：从 processing 中移除消息，代表成功消费
     */
    public void acknowledge(String nodeId, String processingQueueKey, String originalJson) {
        try {
            redisTemplate.opsForList().remove(processingQueueKey, 1, originalJson);
        } catch (Exception e) {
            log.warn("[RMQ_ACK_ERR] nodeId={}", nodeId, e);
        }
    }

    /**
     * 失败回队列：根据 retryCount 决定是否延迟重试
     */
    private void requeue(String nodeId, String processingQueueKey, TaskMessage msg, String originalJson, Exception e) {
        try {
            // 从 processing 移除
            redisTemplate.opsForList().remove(processingQueueKey, 1, originalJson);

            int retry = msg.getRetryCount();
            msg.setRetryCount(retry + 1);

            long now = System.currentTimeMillis();
            long backoff = Math.min(60_000L, 2000L * (long) Math.pow(2, retry)); // 指数退避封顶60s
            long nextAt = now + backoff;

            msg.setScheduledAt(nextAt);

            // 放入 delay zset，等到期再推进
            String delayKey = TASK_DELAY_ZSET_PREFIX + nodeId;
            redisTemplate.opsForZSet().add(delayKey, JSON.toJSONString(msg), nextAt);

            log.warn("[RMQ_REQUEUE] nodeId={} taskId={} retry={} nextAt={} err={}",
                    nodeId, msg.getTaskId(), msg.getRetryCount(), nextAt, e.getMessage());
        } catch (Exception ex) {
            log.error("[RMQ_REQUEUE_ERR] nodeId={}", nodeId, ex);
        }
    }

    /**
     * 对外接口：提交一个“立即执行”的 SysJob（会进入 ready 队列）
     */
    public void enqueueNow(SysJob sysJob, String targetNode, String priority) {
        enqueueAt(sysJob, targetNode, priority, System.currentTimeMillis());
    }

    /**
     * 对外接口：提交一个“指定时间执行”的 SysJob（会进入 delay zset）
     *
     * @param sysJob            任务
     * @param targetNode        目标节点
     * @param priority          优先级 HIGH/NORMAL
     * @param scheduledAtMillis 计划执行时间（毫秒）
     */
    public void enqueueAt(SysJob sysJob, String targetNode, String priority, long scheduledAtMillis) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(String.valueOf(sysJob.getJobId()));

        // 优先使用 SysJob 中的 traceId 作为 executionId (由 Producer Pipeline 统一生成)
        if (StringUtils.isNotEmpty(sysJob.getTraceId())) {
            msg.setExecutionId(sysJob.getTraceId());
        } else {
            // 兜底（理论上 Producer 应必传）
            msg.setExecutionId(UUID.randomUUID().toString());
        }

        msg.setMessageType(TaskMessage.TYPE_EXECUTE);

        msg.setTargetNode(targetNode);
        msg.setJobData(sysJob);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setTraceId(sysJob.getTraceId());
        msg.setPriority(StringUtils.isEmpty(priority) ? "NORMAL" : priority);
        msg.setRetryCount(0);
        msg.setScheduledAt(scheduledAtMillis);

        String nodeId = StringUtils.isEmpty(targetNode) ? currentNodeId : targetNode;
        if (StringUtils.isEmpty(nodeId)) {
            // 没有 nodeId 时退化为当前节点
            nodeId = currentNodeId;
        }

        if (scheduledAtMillis <= System.currentTimeMillis()) {
            pushToReady(nodeId, msg);
        } else {
            String delayKey = TASK_DELAY_ZSET_PREFIX + nodeId;
            redisTemplate.opsForZSet().add(delayKey, JSON.toJSONString(msg), scheduledAtMillis);
        }
    }

    /**
     * 将消息推送到 ready list（按优先级）
     */
    private void pushToReady(String nodeId, TaskMessage msg) {
        String highReady = TASK_QUEUE_PREFIX + nodeId + HIGH_PRIORITY_SUFFIX;
        String normalReady = TASK_QUEUE_PREFIX + nodeId + NORMAL_PRIORITY_SUFFIX;

        String json = JSON.toJSONString(msg);
        if ("HIGH".equalsIgnoreCase(msg.getPriority())) {
            redisTemplate.opsForList().leftPush(highReady, json);
        } else {
            redisTemplate.opsForList().leftPush(normalReady, json);
        }
    }

    /**
     * 推进 delay zset 到期消息进入 ready 队列
     *
     * <p>Scheduled 时间检查嵌入点：队列内部按 scheduledAtMillis(score) 判断是否到期。
     */
    private void promoteDueDelayedMessages(String nodeId, String highReady, String normalReady) {
        String delayKey = TASK_DELAY_ZSET_PREFIX + nodeId;

        long now = System.currentTimeMillis();
        int batch = 50;

        Set<String> due = redisTemplate.opsForZSet().rangeByScore(delayKey, 0, now, 0, batch);
        if (due == null || due.isEmpty()) {
            return;
        }

        // 先 remove 再 push：避免重复推进
        Long removed = redisTemplate.opsForZSet().remove(delayKey, due.toArray());
        if (removed == null || removed <= 0) {
            return;
        }

        for (String json : due) {
            TaskMessage msg = JSON.parseObject(json, TaskMessage.class);
            // 到期直接进入 ready
            if ("HIGH".equalsIgnoreCase(msg.getPriority())) {
                redisTemplate.opsForList().leftPush(highReady, json);
            } else {
                redisTemplate.opsForList().leftPush(normalReady, json);
            }
        }

        log.debug("[RMQ_PROMOTE] nodeId={} promoted={}", nodeId, due.size());
    }

    /**
     * processing 超时回补（避免消费者崩溃导致 processing 堵死）
     */
    private void reclaimProcessingTimeout(String nodeId, String processingQueueKey, String highReady, String normalReady) {
        // 这里使用一个简化策略：
        // 1) processing 队列中消息如果存在超过 timeout，则回补到 delay（立即执行）
        // 2) timeout 由配置决定
        long timeoutMs = 30_000L;

        Long len = redisTemplate.opsForList().size(processingQueueKey);
        if (len == null || len <= 0) return;

        // 批量扫描处理（上限）
        int scan = (int) Math.min(len, 100);

        List<String> items = redisTemplate.opsForList().range(processingQueueKey, -scan, -1);
        if (items == null || items.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (String json : items) {
            try {
                TaskMessage msg = JSON.parseObject(json, TaskMessage.class);
                // 粗略：timestamp 超时即回补（你也可改为 payload 中写入 processingAt）
                if (now - msg.getTimestamp() > timeoutMs) {
                    // 从 processing 移除
                    redisTemplate.opsForList().remove(processingQueueKey, 1, json);
                    // 立即回补到 ready
                    if ("HIGH".equalsIgnoreCase(msg.getPriority())) {
                        redisTemplate.opsForList().leftPush(highReady, json);
                    } else {
                        redisTemplate.opsForList().leftPush(normalReady, json);
                    }
                    log.warn("[RMQ_RECLAIM] nodeId={} taskId={} reclaimed from processing", nodeId, msg.getTaskId());
                }
            } catch (Exception ignore) {
                // ignore parse errors
            }
        }
    }

    // ========= SmartLifecycle =========

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    /**
     * 生命周期优先级：尽量早启动
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    @PreDestroy
    public void shutdown() {
        stopListening();
        if (internalScheduler != null) internalScheduler.shutdownNow();
        if (listenerExecutor != null) listenerExecutor.shutdownNow();
        if (consumerExecutor != null) consumerExecutor.shutdownNow();
    }

    // ========= Message Types =========

    /**
     * 消息处理器接口
     */
    public interface MessageHandler {
        void handle(TaskMessage message) throws Exception;
    }

    /**
     * 队列消息体（包含 scheduledAt 以支持队列内部定时检查）
     */
    public static class TaskMessage {
        public static final String TYPE_EXECUTE = "EXECUTE";
        // TRIGGER type removed as logic is simplified

        private String taskId;
        private String executionId;
        private String targetNode;
        private Object jobData;
        private long timestamp;

        /**
         * 消息类型：EXECUTE
         */
        private String messageType = TYPE_EXECUTE;

        /**
         * 计划执行时间（毫秒）
         */
        private long scheduledAt;

        private Map<String, Object> payload;
        private String traceId;
        private int retryCount = 0;
        private String priority = "NORMAL";

        @JSONField(serialize = false)
        private transient String originalJson;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }

        public String getTargetNode() {
            return targetNode;
        }

        public void setTargetNode(String targetNode) {
            this.targetNode = targetNode;
        }

        public Object getJobData() {
            return jobData;
        }

        public void setJobData(Object jobData) {
            this.jobData = jobData;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getScheduledAt() {
            return scheduledAt;
        }

        public void setScheduledAt(long scheduledAt) {
            this.scheduledAt = scheduledAt;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getOriginalJson() {
            return originalJson;
        }

        public void setOriginalJson(String originalJson) {
            this.originalJson = originalJson;
        }

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }
}
