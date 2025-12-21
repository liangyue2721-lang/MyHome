package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.annotation.JSONField;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.domain.SysJob;
import com.make.quartz.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

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
public class RedisMessageQueue implements org.springframework.context.SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueue.class);

    private static final String TASK_QUEUE_PREFIX = "task:queue:";
    private static final String HIGH_PRIORITY_SUFFIX = ":high";
    private static final String NORMAL_PRIORITY_SUFFIX = ":normal";
    private static final String PROCESSING_QUEUE_SUFFIX = ":processing";
    private static final String RECENT_MONITOR_KEY = "task:monitor:recent";
    public static final String PENDING_TASKS_SET = "task:pending_ids"; // Public for TaskDistributor usage
    public static final String EXECUTING_TASKS_SET = "task:set:executing"; // New Executing Set

    private final RedisTemplate<String, String> redisTemplate;
    private final QuartzProperties quartzProperties;
    private DefaultRedisScript<Long> atomicEnqueueScript;

    private ThreadPoolExecutor executorService;
    private ThreadPoolExecutor stockTaskExecutor;
    private ScheduledExecutorService cleanupScheduler;
    private static RedisMessageQueue instance;
    private volatile String currentNodeId;
    private volatile boolean running = false;

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

        // 任务处理线程池 (Stock Consumer): 负责实际执行任务逻辑
        this.stockTaskExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                quartzProperties.getConsumerCoreSize(),
                quartzProperties.getConsumerMaxSize(),
                quartzProperties.getConsumerQueueCapacity(),
                "stock-consumer-"
        );

        // 清理调度器: 定期回收超时任务
        this.cleanupScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "RedisQueueCleanup");
            t.setDaemon(true);
            return t;
        });

        this.cleanupScheduler.scheduleAtFixedRate(this::cleanupProcessingTasks, 1, 1, TimeUnit.MINUTES);
        this.cleanupScheduler.scheduleAtFixedRate(this::monitorThreadPools, 60, 60, TimeUnit.SECONDS);

        // Load Lua Script
        this.atomicEnqueueScript = new DefaultRedisScript<>();
        this.atomicEnqueueScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/atomic_enqueue.lua")));
        this.atomicEnqueueScript.setResultType(Long.class);

        instance = this;
        log.info("Redis消息队列初始化完成 | Listeners: {} | Executors: {}",
                executorService.getCorePoolSize(), stockTaskExecutor.getCorePoolSize());
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
        sendTaskMessage(taskId, targetNode, jobData, null, null, "NORMAL", true);
    }

    /**
     * 发送任务消息 (带Payload和TraceId)
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData, Map<String, Object> payload, String traceId) {
        sendTaskMessage(taskId, targetNode, jobData, payload, traceId, "NORMAL", true);
    }

    /**
     * 发送任务消息 (全参数, 默认检查唯一性)
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData, Map<String, Object> payload, String traceId, String priority) {
        sendTaskMessage(taskId, targetNode, jobData, payload, traceId, priority, true);
    }

    /**
     * 发送任务消息 (全参数, 可控制唯一性检查)
     * @param checkUniqueness 是否检查任务是否已经在 Pending 状态
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData, Map<String, Object> payload, String traceId, String priority, boolean checkUniqueness) {
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

            if (checkUniqueness) {
                // Use Lua script for atomic Enqueue (Check Set -> Add Set -> Push List)
                // KEYS[1]: PENDING_SET, KEYS[2]: QUEUE_LIST
                // ARGV[1]: taskId, ARGV[2]: messageJson
                Long result = redisTemplate.execute(atomicEnqueueScript,
                        java.util.Arrays.asList(PENDING_TASKS_SET, queueKey),
                        taskId, messageJson);

                if (result != null && result == 0) {
                     log.warn("[QUEUE_SEND_DUPLICATE] 任务 {} 已在 Pending 队列中 (Lua)，忽略重复发送", taskId);
                     return;
                }
            } else {
                // 如果是 Transfer 或 Requeue，确保它在 Set 中（防止数据不一致）
                redisTemplate.opsForSet().add(PENDING_TASKS_SET, taskId);
                redisTemplate.opsForList().leftPush(queueKey, messageJson);
            }

            log.info("[QUEUE_ENQUEUE] 任务入队成功 | Queue: {} | TaskID: {} | Priority: {} | TraceId: {}",
                    queueKey, taskId, priority, traceId);
        } catch (Exception e) {
            log.error("[QUEUE_SEND_ERROR] 发送任务消息失败 | TaskID: {}", taskId, e);
            // 失败回滚 Set 状态（虽然可能是网络超时实际成功了，但这里简单移除以允许重试）
            if (checkUniqueness) {
                redisTemplate.opsForSet().remove(PENDING_TASKS_SET, taskId);
            }
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
            while (running && !Thread.currentThread().isInterrupted()) {
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
                } catch (org.springframework.data.redis.RedisConnectionFailureException | org.springframework.data.redis.RedisSystemException e) {
                    log.warn("[QUEUE_LISTEN_WARN] Redis连接中断，尝试重连: {}", e.getMessage());
                    sleep(2000);
                } catch (Exception e) {
                    // Check specifically for Redis command interruption which can happen on client close
                    if (e instanceof io.lettuce.core.RedisCommandInterruptedException || e.getCause() instanceof InterruptedException) {
                        log.info("[QUEUE_LISTEN_STOP] 监听线程被中断，停止监听");
                        Thread.currentThread().interrupt();
                        break;
                    }
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

        // 关键：任务开始处理
        // 1. 从 Pending Set 中移除
        // 2. 添加到 Executing Set
        try {
            redisTemplate.opsForSet().remove(PENDING_TASKS_SET, message.getTaskId());
            redisTemplate.opsForSet().add(EXECUTING_TASKS_SET, message.getTaskId());
        } catch (Exception e) {
            log.warn("[QUEUE_SET_UPDATE_FAIL] 更新任务集合状态失败 | TaskID: {}", message.getTaskId(), e);
        }

        if (isTaskRunningLocally(message.getTaskId())) {
            log.info("[QUEUE_MSG_DUPLICATE] 任务 {} 已在本地运行，自动确认消息", message.getTaskId());
            acknowledge(message);
            return;
        }

        try {
            // 监控任务堆积情况 (80% of ConsumerQueueCapacity)
            int capacity = quartzProperties.getConsumerQueueCapacity();
            if (stockTaskExecutor.getQueue().size() > (capacity * 0.8)) {
                log.warn("[QUEUE_HIGH_LOAD] 任务处理队列积压严重 | QueueSize: {}/{}",
                        stockTaskExecutor.getQueue().size(), capacity);
            }

        // Enforce Strict Consumer Thread Pool Usage with Blocking Retry
        // Do NOT fall back to caller runs to prevent mixing logic in Scheduler/Listener pools.
        while (!Thread.currentThread().isInterrupted()) {
                try {
                stockTaskExecutor.submit(() -> {
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
                // Submission successful, break loop
                break;
            } catch (java.util.concurrent.RejectedExecutionException e) {
                log.warn("[QUEUE_PROCESS_FULL] 股票任务线程池已满，等待空闲... | TaskID: {}", message.getTaskId());
                // Backoff wait to prevent CPU spin
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        } catch (Exception e) {
            log.error("[QUEUE_PROCESS_FATAL] 提交任务失败", e);
        }
    }

    /**
     * 确认消息 (ACK): 从 processing 队列移除并记录成功状态
     */
    public void acknowledge(TaskMessage message) {
        if (message == null || StringUtils.isEmpty(message.getOriginalJson())) return;
        try {
            deleteFromProcessingQueue(message);

            // 任务完成，从 Executing Set 移除
            redisTemplate.opsForSet().remove(EXECUTING_TASKS_SET, message.getTaskId());

            recordRecentTask(message, "SUCCESS");
            log.debug("[QUEUE_ACK] 消息已确认移除 | TaskID: {}", message.getTaskId());
        } catch (Exception e) {
            log.error("[QUEUE_ACK_ERROR] 确认消息失败 | TaskID: {}", message.getTaskId(), e);
        }
    }

    /**
     * 从处理队列中删除消息
     */
    private void deleteFromProcessingQueue(TaskMessage message) {
        String processingQueueKey = TASK_QUEUE_PREFIX + message.getTargetNode() + PROCESSING_QUEUE_SUFFIX;
        redisTemplate.opsForList().remove(processingQueueKey, 1, message.getOriginalJson());
    }

    /**
     * 记录最近完成的任务 (Success/Fail)
     */
    private void recordRecentTask(TaskMessage message, String status) {
        try {
            Map<String, Object> record = new java.util.HashMap<>();
            record.put("taskId", message.getTaskId());
            record.put("targetNode", message.getTargetNode());
            record.put("priority", message.getPriority());
            record.put("traceId", message.getTraceId());
            record.put("enqueueTime", message.getTimestamp());
            record.put("completionTime", System.currentTimeMillis());
            record.put("status", status);
            record.put("retryCount", message.getRetryCount());

            String json = JSON.toJSONString(record);
            redisTemplate.opsForList().leftPush(RECENT_MONITOR_KEY, json);
            redisTemplate.opsForList().trim(RECENT_MONITOR_KEY, 0, 49); // Keep last 50
        } catch (Exception e) {
            log.error("[MONITOR_REC_ERROR] 记录最近任务失败", e);
        }
    }

    /**
     * 重新入队 (Retry): 失败重试
     */
    public void requeue(TaskMessage message) {
        if (message == null) return;
        try {
            // 先从 processing 队列移除
            deleteFromProcessingQueue(message);

            // 失败重试，从 Executing Set 移除
            redisTemplate.opsForSet().remove(EXECUTING_TASKS_SET, message.getTaskId());

            // 检查最大重试次数
            if (message.getRetryCount() >= quartzProperties.getMaxRetryCount()) {
                log.error("[QUEUE_RETRY_LIMIT] 任务 {} 超过最大重试次数 ({})，丢弃任务",
                        message.getTaskId(), quartzProperties.getMaxRetryCount());
                recordRecentTask(message, "FAIL");
                return;
            }

            // 重新标记为 Pending
            redisTemplate.opsForSet().add(PENDING_TASKS_SET, message.getTaskId());

            message.setRetryCount(message.getRetryCount() + 1);
            message.setTimestamp(System.currentTimeMillis()); // Update enqueue time for retry

            String messageJson = JSON.toJSONString(message);
            String queueKey = getQueueKey(message.getTargetNode(), message.getPriority());

            // 重新推入等待队列
            redisTemplate.opsForList().leftPush(queueKey, messageJson);
            log.info("[QUEUE_REQUEUE] 任务已重新入队 | TaskID: {} | Retry: {}/{}",
                    message.getTaskId(), message.getRetryCount(), quartzProperties.getMaxRetryCount());
        } catch (Exception e) {
            log.error("[QUEUE_REQUEUE_ERROR] 重新入队失败 | TaskID: {}", message.getTaskId(), e);
            // 如果入队失败，尝试回滚 Pending 状态（虽然已经迟了）
             redisTemplate.opsForSet().remove(PENDING_TASKS_SET, message.getTaskId());
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

    /**
     * 获取全局任务统计信息 (Pending & Executing)
     */
    public Map<String, Long> getGlobalTaskCounts() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", 0L);
        stats.put("executing", 0L);

        try {
            // Pending Count = Size of Unique Pending ID Set
            Long pendingCount = redisTemplate.opsForSet().size(PENDING_TASKS_SET);
            stats.put("pending", pendingCount != null ? pendingCount : 0L);

            // Executing Count = Size of Unique Executing ID Set
            Long executingCount = redisTemplate.opsForSet().size(EXECUTING_TASKS_SET);
            stats.put("executing", executingCount != null ? executingCount : 0L);

        } catch (Exception e) {
            log.error("获取全局任务统计失败", e);
        }
        return stats;
    }

    /**
     * 获取所有队列中的详细任务信息 (For Monitoring)
     * 使用 SCAN 替代 KEYS 避免阻塞，并限制返回数量防止 OOM
     */
    public java.util.List<java.util.Map<String, Object>> getAllQueueDetails() {
        java.util.List<java.util.Map<String, Object>> allTasks = new java.util.ArrayList<>();
        Set<String> visitedTaskIds = new HashSet<>(); // 过滤重复任务ID

        try {
            // 使用 SCAN 命令查找匹配的 Key
            java.util.Set<String> keys = new java.util.HashSet<>();
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions().match(TASK_QUEUE_PREFIX + "*").count(100).build()
                );
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
                return null;
            });

            if (!keys.isEmpty()) {
                for (String key : keys) {
                    // 限制每个队列最多读取前 50 条消息
                    java.util.List<String> messages = redisTemplate.opsForList().range(key, 0, 49);
                    if (messages != null) {
                        for (String msgJson : messages) {
                            try {
                                TaskMessage task = JSON.parseObject(msgJson, TaskMessage.class);
                                java.util.Map<String, Object> taskMap = new java.util.HashMap<>();
                                String taskId = null;

                                // 处理 SysJob 类型的消息 (taskId 为空)
                                if (task.getTaskId() == null) {
                                    try {
                                        SysJob sysJob = JSON.parseObject(msgJson, SysJob.class);
                                        if (sysJob != null && sysJob.getJobId() != null) {
                                            taskId = sysJob.getJobId() + "." + sysJob.getJobName();
                                            taskMap.put("targetNode", null); // 全局队列中无目标节点
                                            taskMap.put("priority", sysJob.getPriority());
                                            taskMap.put("traceId", sysJob.getTraceId());
                                            taskMap.put("enqueueTime", sysJob.getEnqueueTime());
                                            taskMap.put("retryCount", 0);
                                        }
                                    } catch (Exception ex) {
                                        // Ignore
                                    }
                                } else {
                                    taskId = task.getTaskId();
                                    taskMap.put("targetNode", task.getTargetNode());
                                    taskMap.put("priority", task.getPriority());
                                    taskMap.put("traceId", task.getTraceId());
                                    taskMap.put("enqueueTime", task.getTimestamp());
                                    taskMap.put("retryCount", task.getRetryCount());
                                }

                                if (taskId == null) continue;
                                taskMap.put("taskId", taskId);
                                taskMap.put("queueName", key);

                                // 过滤重复: 如果已经在列表中，跳过
                                if (key.endsWith(PROCESSING_QUEUE_SUFFIX)) {
                                    taskMap.put("status", "PROCESSING");
                                    // Processing tasks are always included
                                    allTasks.add(taskMap);
                                } else {
                                    taskMap.put("status", "WAITING");
                                    // Pending tasks: filter duplicates
                                    if (visitedTaskIds.add(taskId)) {
                                        allTasks.add(taskMap);
                                    }
                                }

                                // 总条数限制保护 (例如 500 条)
                                if (allTasks.size() >= 500) {
                                    return allTasks;
                                }
                            } catch (Exception e) {
                                // Ignore parse error for monitoring
                            }
                        }
                    }
                }
            }

            // 获取最近完成的任务
            java.util.List<String> recentTasks = redisTemplate.opsForList().range(RECENT_MONITOR_KEY, 0, -1);
            if (recentTasks != null) {
                for (String msgJson : recentTasks) {
                    try {
                        java.util.Map<String, Object> taskMap = JSON.parseObject(msgJson, java.util.Map.class);
                        taskMap.put("queueName", "RECENT_HISTORY");
                        allTasks.add(taskMap);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取队列详情失败", e);
        }
        return allTasks;
    }

    public void stopListening() {
        this.running = false;
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (stockTaskExecutor != null) stockTaskExecutor.shutdownNow();
        if (cleanupScheduler != null) cleanupScheduler.shutdownNow();
    }

    private void monitorThreadPools() {
        if (!running) return;
        try {
            long queueSize = getLocalQueueLength();
            log.info("[MONITOR] Pool: RedisQueueListener | Active: {}/{} | Queue: N/A | RedisQueue: {}",
                    executorService.getActiveCount(), executorService.getCorePoolSize(), queueSize);

            log.info("[MONITOR] Pool: StockTaskExecutor | Active: {}/{} | Queue: {}",
                    stockTaskExecutor.getActiveCount(), stockTaskExecutor.getCorePoolSize(), stockTaskExecutor.getQueue().size());
        } catch (Exception e) {
            log.error("[MONITOR_ERROR] 监控指标采集失败", e);
        }
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        log.info("停止Redis消息队列监听...");
        stopListening();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // 确保最先停止
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
