package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.annotation.JSONField;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的可靠消息队列实现 (Reliable Redis Message Queue)
 *
 * 核心机制：
 * 1. **Reliable Consumption (RPOPLPUSH)**:
 *    使用 Redis 的 `RPOPLPUSH` 命令将任务从 "等待队列" 原子移动到 "处理中队列" (Processing Queue)。
 *    这确保了即使消费者在处理过程中崩溃，任务也不会丢失，而是保留在处理中队列中。
 *
 * 2. **Acknowledgement (ACK)**:
 *    任务执行成功后，消费者显式调用 `acknowledge()`，将任务从处理中队列移除。
 *
 * 3. **Retry (Requeue)**:
 *    如果任务执行失败，调用 `requeue()` 将任务重新放回等待队列（增加重试计数）。
 *
 * 4. **Auto Cleanup**:
 *    后台线程定期扫描处理中队列，发现超时未完成的任务（判定为消费者崩溃），自动将其重新入队。
 *
 * 5. **Priority Queues**:
 *    支持 HIGH (高) 和 NORMAL (普通) 优先级队列。消费者优先消费高优先级队列。
 */
@Component
public class RedisMessageQueue {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueue.class);

    private static final String TASK_QUEUE_PREFIX = "task:queue:";
    private static final String HIGH_PRIORITY_SUFFIX = ":high";
    private static final String NORMAL_PRIORITY_SUFFIX = ":normal";
    private static final String PROCESSING_QUEUE_SUFFIX = ":processing";

    private final RedisTemplate<String, String> redisTemplate;
    private final QuartzProperties quartzProperties;

    private ThreadPoolExecutor executorService;
    private ThreadPoolExecutor taskProcessExecutor;
    private ScheduledExecutorService cleanupScheduler;
    private static RedisMessageQueue instance;
    private volatile String currentNodeId;

    // 本地去重缓存: 防止同一节点重复执行相同任务
    private static final ConcurrentHashMap<String, Long> processingMessages = new ConcurrentHashMap<>();

    @Autowired
    public RedisMessageQueue(RedisTemplate<String, String> redisTemplate, QuartzProperties quartzProperties) {
        this.redisTemplate = redisTemplate;
        this.quartzProperties = quartzProperties;
    }

    @PostConstruct
    public void init() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        // 监听线程池: 负责阻塞监听 Redis 队列
        this.executorService = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                cpuCores * quartzProperties.getListenerCoreThreadsMultiple(),
                cpuCores * quartzProperties.getListenerMaxThreadsMultiple(),
                200,
                "RedisQueueListener"
        );

        // 任务处理线程池: 负责实际执行任务逻辑
        this.taskProcessExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                cpuCores * quartzProperties.getExecutorCoreThreadsMultiple(),
                cpuCores * quartzProperties.getExecutorMaxThreadsMultiple(),
                10000,
                "TaskProcessor"
        );

        // 清理调度器: 定期回收超时任务
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RedisQueueCleanup");
            t.setDaemon(true);
            return t;
        });

        this.cleanupScheduler.scheduleAtFixedRate(this::cleanupProcessingTasks, 1, 1, TimeUnit.MINUTES);

        instance = this;
        log.info("Redis消息队列初始化完成 | Listeners: {} | Executors: {}",
                executorService.getCorePoolSize(), taskProcessExecutor.getCorePoolSize());
    }

    public static RedisMessageQueue getInstance() {
        if (instance == null) {
            instance = SpringUtils.getBean(RedisMessageQueue.class);
        }
        return instance;
    }

    /**
     * 发送任务消息 (默认普通优先级)
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData) {
        sendTaskMessage(taskId, targetNode, jobData, null, null, "NORMAL");
    }

    /**
     * 发送任务消息 (带Payload和TraceId)
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData, Map<String, Object> payload, String traceId) {
        sendTaskMessage(taskId, targetNode, jobData, payload, traceId, "NORMAL");
    }

    /**
     * 发送任务消息 (全参数)
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData, Map<String, Object> payload, String traceId, String priority) {
        try {
            if (isTaskRunningLocally(taskId)) {
                log.warn("[QUEUE_SEND_SKIP] 任务 {} 已在本地运行中，不重复发送消息", taskId);
                return;
            }

            TaskMessage message = new TaskMessage();
            message.setTaskId(taskId);
            message.setTargetNode(targetNode);
            message.setJobData(jobData);
            message.setPayload(payload);
            message.setTraceId(traceId);
            message.setPriority(priority);
            message.setTimestamp(System.currentTimeMillis());
            message.setRetryCount(0);

            String messageJson = JSON.toJSONString(message);
            String queueKey = getQueueKey(targetNode, priority);

            redisTemplate.opsForList().leftPush(queueKey, messageJson);
            log.info("[QUEUE_ENQUEUE] 任务入队成功 | Queue: {} | TaskID: {} | Priority: {} | TraceId: {}",
                    queueKey, taskId, priority, traceId);
        } catch (Exception e) {
            log.error("[QUEUE_SEND_ERROR] 发送任务消息失败 | TaskID: {}", taskId, e);
        }
    }

    private String getQueueKey(String targetNode, String priority) {
        return "HIGH".equalsIgnoreCase(priority) ?
               TASK_QUEUE_PREFIX + targetNode + HIGH_PRIORITY_SUFFIX :
               TASK_QUEUE_PREFIX + targetNode + NORMAL_PRIORITY_SUFFIX;
    }

    /**
     * 启动消息监听
     */
    public void startListening(String nodeId, MessageHandler messageHandler) {
        this.currentNodeId = nodeId;
        String highPriorityQueue = TASK_QUEUE_PREFIX + nodeId + HIGH_PRIORITY_SUFFIX;
        String normalPriorityQueue = TASK_QUEUE_PREFIX + nodeId + NORMAL_PRIORITY_SUFFIX;
        String processingQueueKey = TASK_QUEUE_PREFIX + nodeId + PROCESSING_QUEUE_SUFFIX;
        String legacyQueue = TASK_QUEUE_PREFIX + nodeId;

        log.info("启动消息监听器 | NodeID: {} | ProcessingQueue: {}", nodeId, processingQueueKey);

        executorService.submit(() -> {
            log.info("消息监听线程已启动");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 1. 优先尝试高优先级队列
                    String messageJson = redisTemplate.opsForList().rightPopAndLeftPush(highPriorityQueue, processingQueueKey, 2, TimeUnit.SECONDS);

                    // 2. 如果没有高优先级任务，尝试普通优先级
                    if (messageJson == null) {
                        messageJson = redisTemplate.opsForList().rightPopAndLeftPush(normalPriorityQueue, processingQueueKey, 2, TimeUnit.SECONDS);
                    }

                    // 3. 兼容旧队列
                    if (messageJson == null) {
                         messageJson = redisTemplate.opsForList().rightPopAndLeftPush(legacyQueue, processingQueueKey, 1, TimeUnit.SECONDS);
                    }

                    if (messageJson != null) {
                        // 日志记录：收到消息，已进入 processing 队列
                        if (log.isDebugEnabled()) {
                            log.debug("[QUEUE_RECEIVE] 接收到消息并移入处理队列 | Msg: {}", StringUtils.substring(messageJson, 0, 100));
                        }
                        processReceivedMessage(messageJson, messageHandler, processingQueueKey);
                    }
                } catch (Exception e) {
                    log.error("[QUEUE_LISTEN_ERROR] 消息监听循环异常", e);
                    sleep(1000);
                }
            }
        });
    }

    private void processReceivedMessage(String messageJson, MessageHandler messageHandler, String processingQueueKey) {
        TaskMessage message = parseMessage(messageJson);
        if (message == null) {
            log.warn("[QUEUE_MSG_INVALID] 无法解析消息，移除 | Msg: {}", messageJson);
            redisTemplate.opsForList().remove(processingQueueKey, 1, messageJson);
            return;
        }

        if (isTaskRunningLocally(message.getTaskId())) {
            log.info("[QUEUE_MSG_DUPLICATE] 任务 {} 已在本地运行，自动确认消息", message.getTaskId());
            acknowledge(message);
            return;
        }

        taskProcessExecutor.submit(() -> {
            processingMessages.put(message.getTaskId(), System.currentTimeMillis());
            try {
                log.info("[QUEUE_PROCESS_START] 开始处理任务 | TaskID: {} | TraceId: {}", message.getTaskId(), message.getTraceId());
                messageHandler.handleMessage(message);

                // 执行成功 -> ACK
                acknowledge(message);
                log.info("[QUEUE_PROCESS_SUCCESS] 任务处理完成并确认 | TaskID: {}", message.getTaskId());
            } catch (Exception e) {
                log.error("[QUEUE_PROCESS_ERROR] 处理任务异常 | TaskID: {}", message.getTaskId(), e);
                // 执行失败 -> Requeue
                requeue(message);
            } finally {
                processingMessages.remove(message.getTaskId());
            }
        });
    }

    /**
     * 确认消息 (ACK): 从 processing 队列移除
     */
    public void acknowledge(TaskMessage message) {
        if (message == null || StringUtils.isEmpty(message.getOriginalJson())) return;
        try {
            String processingQueueKey = TASK_QUEUE_PREFIX + message.getTargetNode() + PROCESSING_QUEUE_SUFFIX;
            redisTemplate.opsForList().remove(processingQueueKey, 1, message.getOriginalJson());
            log.debug("[QUEUE_ACK] 消息已确认移除 | TaskID: {}", message.getTaskId());
        } catch (Exception e) {
            log.error("[QUEUE_ACK_ERROR] 确认消息失败 | TaskID: {}", message.getTaskId(), e);
        }
    }

    /**
     * 重新入队 (Retry): 失败重试
     */
    public void requeue(TaskMessage message) {
        if (message == null) return;
        try {
            // 先从 processing 队列移除
            acknowledge(message);

            // 检查最大重试次数
            if (message.getRetryCount() >= quartzProperties.getMaxRetryCount()) {
                log.error("[QUEUE_RETRY_LIMIT] 任务 {} 超过最大重试次数 ({})，丢弃任务",
                        message.getTaskId(), quartzProperties.getMaxRetryCount());
                return;
            }

            message.setRetryCount(message.getRetryCount() + 1);
            message.setTimestamp(System.currentTimeMillis());

            String messageJson = JSON.toJSONString(message);
            String queueKey = getQueueKey(message.getTargetNode(), message.getPriority());

            // 重新推入等待队列
            redisTemplate.opsForList().leftPush(queueKey, messageJson);
            log.info("[QUEUE_REQUEUE] 任务已重新入队 | TaskID: {} | Retry: {}/{}",
                    message.getTaskId(), message.getRetryCount(), quartzProperties.getMaxRetryCount());
        } catch (Exception e) {
            log.error("[QUEUE_REQUEUE_ERROR] 重新入队失败 | TaskID: {}", message.getTaskId(), e);
        }
    }

    /**
     * 清理超时任务 (Crash Recovery)
     */
    private void cleanupProcessingTasks() {
        if (StringUtils.isEmpty(currentNodeId)) return;
        String processingQueueKey = TASK_QUEUE_PREFIX + currentNodeId + PROCESSING_QUEUE_SUFFIX;

        try {
            java.util.List<String> messages = redisTemplate.opsForList().range(processingQueueKey, 0, -1);
            if (messages == null || messages.isEmpty()) return;

            long now = System.currentTimeMillis();
            int recoveredCount = 0;

            for (String msgJson : messages) {
                TaskMessage message = parseMessage(msgJson);
                if (message == null) {
                    redisTemplate.opsForList().remove(processingQueueKey, 1, msgJson);
                    continue;
                }

                // 检查是否超时
                if (now - message.getTimestamp() > quartzProperties.getTaskTimeout()) {
                    // 再次确认是否正在本地运行 (避免误杀长任务)
                    if (!isTaskRunningLocally(message.getTaskId())) {
                        log.warn("[QUEUE_CLEANUP] 发现超时任务，准备回收 | TaskID: {} | Timeout: {}ms",
                                message.getTaskId(), now - message.getTimestamp());
                        requeue(message);
                        recoveredCount++;
                    }
                }
            }
            if (recoveredCount > 0) {
                log.info("[QUEUE_CLEANUP_SUMMARY] 已回收 {} 个超时任务", recoveredCount);
            }
        } catch (Exception e) {
            log.error("[QUEUE_CLEANUP_ERROR] 清理队列异常", e);
        }
    }

    public long getLocalQueueLength() {
        if (StringUtils.isEmpty(currentNodeId)) return 0;
        try {
            String highKey = TASK_QUEUE_PREFIX + currentNodeId + HIGH_PRIORITY_SUFFIX;
            String normalKey = TASK_QUEUE_PREFIX + currentNodeId + NORMAL_PRIORITY_SUFFIX;
            Long h = redisTemplate.opsForList().size(highKey);
            Long n = redisTemplate.opsForList().size(normalKey);
            return (h != null ? h : 0) + (n != null ? n : 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public void stopListening() {
        if (executorService != null) executorService.shutdownNow();
        if (taskProcessExecutor != null) taskProcessExecutor.shutdownNow();
        if (cleanupScheduler != null) cleanupScheduler.shutdownNow();
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private TaskMessage parseMessage(String messageContent) {
        try {
            TaskMessage msg = JSON.parseObject(messageContent, TaskMessage.class);
            if (msg != null) msg.setOriginalJson(messageContent);
            return msg;
        } catch (JSONException e) {
            try {
                TaskMessage message = new TaskMessage();
                message.setTaskId(messageContent);
                message.setTargetNode(currentNodeId);
                message.setTimestamp(System.currentTimeMillis());
                message.setOriginalJson(messageContent);
                return message;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static boolean isMessageProcessing(String taskId) {
        return processingMessages.containsKey(taskId);
    }

    private boolean isTaskRunningLocally(String taskId) {
        return processingMessages.containsKey(taskId) ||
               AbstractQuartzJob.isJobExecuting(taskId) ||
               TaskExecutionService.isTaskExecuting(taskId);
    }

    public static class TaskMessage {
        private String taskId;
        private String targetNode;
        private Object jobData;
        private long timestamp;
        private Map<String, Object> payload;
        private String traceId;
        private int retryCount = 0;
        private String priority = "NORMAL";
        @JSONField(serialize = false)
        private transient String originalJson;

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTargetNode() { return targetNode; }
        public void setTargetNode(String targetNode) { this.targetNode = targetNode; }
        public Object getJobData() { return jobData; }
        public void setJobData(Object jobData) { this.jobData = jobData; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getOriginalJson() { return originalJson; }
        public void setOriginalJson(String originalJson) { this.originalJson = originalJson; }
    }

    public interface MessageHandler {
        void handleMessage(TaskMessage message);
    }
}
