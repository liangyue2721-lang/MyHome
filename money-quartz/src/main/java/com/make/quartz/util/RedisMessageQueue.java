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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * Redis 队列调度与消费中心（Redis-only 引擎）
 *
 * <p>实现目标：
 * 1) 任务统一进入 Redis 全局队列（Global Queue）
 * 2) 队列内部嵌入 Scheduled 时间检查：使用 Global Delay ZSET
 * 3) 多实例竞争：每个节点通过 Lua 脚本原子抢占任务，并标记 Owner
 *
 * <p>结构：
 * - 全局队列：mq:task:global:high / mq:task:global:normal
 * - 全局 Delay ZSET：mq:delay:global
 * - 处理中队列（归属）：mq:task:processing:{ownerIp}
 * - 任务元数据：mq:task:meta:{executionId} (owner, processingAt)
 */
@Component
public class RedisMessageQueue implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueue.class);

    private static volatile RedisMessageQueue instance;

    private final RedisTemplate<String, String> redisTemplate;
    private final QuartzProperties quartzProperties;

    // Keys
    private static final String GLOBAL_QUEUE_HIGH = "mq:task:global:high";
    private static final String GLOBAL_QUEUE_NORMAL = "mq:task:global:normal";
    private static final String GLOBAL_DELAY_ZSET = "mq:delay:global";
    private static final String PROCESSING_PREFIX = "mq:task:processing:";
    private static final String META_PREFIX = "mq:task:meta:";
    private static final String RECLAIM_LOCK_PREFIX = "mq:task:reclaim:lock:";
    private static final String LOCAL_RETRY_PREFIX = "mq:retry:";
    private static final String NODE_FUSED_PREFIX = "mq:node:fused:";

    /**
     * 是否运行（生命周期）
     */
    private volatile boolean running = false;

    /**
     * 当前节点 ID (IP)
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

    // Lua Scripts
    private DefaultRedisScript<String> pollScript;
    private DefaultRedisScript<String> pollLocalRetryScript;
    private DefaultRedisScript<Long> promoteScript;
    private DefaultRedisScript<Long> reclaimScript;

    public RedisMessageQueue(RedisTemplate<String, String> redisTemplate, QuartzProperties quartzProperties) {
        this.redisTemplate = redisTemplate;
        this.quartzProperties = quartzProperties;
    }

    /**
     * 单例获取
     */
    public static RedisMessageQueue getInstance() {
        if (instance == null) {
            instance = SpringUtils.getBean(RedisMessageQueue.class);
        }
        return instance;
    }

    @PostConstruct
    public void init() {
        // Initialize Lua Scripts
        initLuaScripts();

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
                "redis-queue-consumer",
                new ThreadPoolExecutor.AbortPolicy() // Use Abort to enable backpressure
        );

        this.internalScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "redis-queue-internal-scheduler");
            t.setDaemon(true);
            return t;
        });

        // 1. 启动定时任务：推进 Delayed 消息 (Global)
        this.internalScheduler.scheduleWithFixedDelay(() -> {
            if (running) {
                try {
                    promoteDueDelayedMessages();
                } catch (Exception e) {
                    log.warn("[RMQ_PROMOTE_ERR] {}", e.getMessage());
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        // 2. 启动定时任务：全局回补 Processing 超时消息
        this.internalScheduler.scheduleWithFixedDelay(() -> {
            if (running) {
                try {
                    reclaimGlobalProcessingTimeout();
                } catch (Exception e) {
                    log.warn("[RMQ_RECLAIM_ERR] {}", e.getMessage());
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        log.info("[RMQ_INIT] RedisMessageQueue init done (Competitive Mode). listenerCore={}, consumerCore={}",
                listenerExecutor.getCorePoolSize(), consumerExecutor.getCorePoolSize());
    }

    private void initLuaScripts() {
        // Script: POLL_TASK
        // KEYS[1]=GlobalHigh, KEYS[2]=GlobalNormal
        // ARGV[1]=ownerIp, ARGV[2]=nowMillis, ARGV[3]=metaPrefix, ARGV[4]=processingPrefix
        String pollLua = "local msg = redis.call('RPOP', KEYS[1])\n" +
                "local src = 'HIGH'\n" +
                "if not msg then\n" +
                "    msg = redis.call('RPOP', KEYS[2])\n" +
                "    src = 'NORMAL'\n" +
                "end\n" +
                "if not msg then return nil end\n" +
                "\n" +
                "local p = string.find(msg, \"\\n\")\n" +
                "if not p then\n" +
                "    -- Invalid format, move to dead letter queue\n" +
                "    redis.call('LPUSH', 'mq:task:dead', msg)\n" +
                "    return nil\n" +
                "end\n" +
                "\n" +
                "local header = string.sub(msg, 1, p-1)\n" +
                "local executionId = header\n" +
                "local sep = string.find(header, \"|\")\n" +
                "if sep then executionId = string.sub(header, 1, sep-1) end\n" +
                "\n" +
                "local metaKey = ARGV[3] .. executionId\n" +
                "local processingKey = ARGV[4] .. ARGV[1]\n" +
                "\n" +
                "redis.call('HSET', metaKey, 'owner', ARGV[1], 'processingAt', ARGV[2])\n" +
                "redis.call('LPUSH', processingKey, msg)\n" +
                "\n" +
                "return msg";
        this.pollScript = new DefaultRedisScript<>(pollLua, String.class);

        // Script: POLL_LOCAL_RETRY
        // KEYS[1]=LocalRetryZSet
        // ARGV[1]=ownerIp, ARGV[2]=nowMillis, ARGV[3]=metaPrefix, ARGV[4]=processingPrefix
        String pollLocalLua = "local now = tonumber(ARGV[2])\n" +
                "local msgs = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, 1)\n" +
                "if #msgs == 0 then return nil end\n" +
                "\n" +
                "local msg = msgs[1]\n" +
                "redis.call('ZREM', KEYS[1], msg)\n" +
                "\n" +
                "local p = string.find(msg, \"\\n\")\n" +
                "if not p then return nil end\n" +
                "\n" +
                "local header = string.sub(msg, 1, p-1)\n" +
                "local executionId = header\n" +
                "local sep = string.find(header, \"|\")\n" +
                "if sep then executionId = string.sub(header, 1, sep-1) end\n" +
                "\n" +
                "local metaKey = ARGV[3] .. executionId\n" +
                "local processingKey = ARGV[4] .. ARGV[1]\n" +
                "\n" +
                "redis.call('HSET', metaKey, 'owner', ARGV[1], 'processingAt', ARGV[2])\n" +
                "redis.call('LPUSH', processingKey, msg)\n" +
                "\n" +
                "return msg";
        this.pollLocalRetryScript = new DefaultRedisScript<>(pollLocalLua, String.class);

        // Script: PROMOTE_TASK
        // KEYS[1]=DelayZSet, KEYS[2]=GlobalHigh, KEYS[3]=GlobalNormal
        // ARGV[1]=nowMillis, ARGV[2]=batch
        String promoteLua = "local now = tonumber(ARGV[1])\n" +
                "local batch = tonumber(ARGV[2])\n" +
                "local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, batch)\n" +
                "if (#due == 0) then return 0 end\n" +
                "\n" +
                "local pushed = 0\n" +
                "for i, msg in ipairs(due) do\n" +
                "    if redis.call('ZREM', KEYS[1], msg) == 1 then\n" +
                "        local p = string.find(msg, \"\\n\")\n" +
                "        local priority = 'NORMAL'\n" +
                "        if p then\n" +
                "            local header = string.sub(msg, 1, p-1)\n" +
                "            local sep = string.find(header, \"|\")\n" +
                "            if sep then\n" +
                "                 local pri = string.sub(header, sep+1)\n" +
                "                 if pri == 'HIGH' then priority = 'HIGH' end\n" +
                "            end\n" +
                "        end\n" +
                "        if priority == 'HIGH' then\n" +
                "            redis.call('LPUSH', KEYS[2], msg)\n" +
                "        else\n" +
                "            redis.call('LPUSH', KEYS[3], msg)\n" +
                "        end\n" +
                "        pushed = pushed + 1\n" +
                "    end\n" +
                "end\n" +
                "return pushed";
        this.promoteScript = new DefaultRedisScript<>(promoteLua, Long.class);

        // Script: RECLAIM_TASK
        // KEYS[1]=processingQueue, KEYS[2]=metaKey, KEYS[3]=TargetQueue
        // ARGV[1]=rawMsg
        String reclaimLua = "local removed = redis.call('LREM', KEYS[1], 1, ARGV[1])\n" +
                "if removed > 0 then\n" +
                "    redis.call('DEL', KEYS[2])\n" +
                "    redis.call('LPUSH', KEYS[3], ARGV[1])\n" +
                "    return 1\n" +
                "end\n" +
                "return 0";
        this.reclaimScript = new DefaultRedisScript<>(reclaimLua, Long.class);
    }

    /**
     * 启动监听（Global Competition）
     *
     * @param nodeId  当前节点ID (IP) - 仅作为 Owner 标识
     * @param handler 消息处理器
     */
    public void startListening(String nodeId, MessageHandler handler) {
        this.currentNodeId = nodeId;
        this.running = true;

        int concurrency = quartzProperties.getListenerConcurrency();
        if (concurrency <= 0) concurrency = 1;

        log.info("[RMQ_START] Starting global listening on node={} concurrency={}", currentNodeId, concurrency);

        for (int i = 0; i < concurrency; i++) {
            listenerExecutor.submit(() -> runListenerLoop(handler));
        }
    }

    private void runListenerLoop(MessageHandler handler) {
        while (running && !Thread.currentThread().isInterrupted()) {
            TaskMessage msg = null;
            try {
                // 1. Poll Local Retry (Priority 1)
                // Always check local retry first, even if fused (it's our own mess to clean up)
                String msgRaw = redisTemplate.execute(pollLocalRetryScript,
                        Collections.singletonList(LOCAL_RETRY_PREFIX + currentNodeId),
                        currentNodeId, String.valueOf(System.currentTimeMillis()), META_PREFIX, PROCESSING_PREFIX);

                // 2. Poll Global (Priority 2)
                if (msgRaw == null) {
                    // Check Circuit Breaker
                    if (redisTemplate.hasKey(NODE_FUSED_PREFIX + currentNodeId)) {
                        // Node is FUSED: Skip global poll, sleep and continue
                        Thread.sleep(1000);
                        continue;
                    }

                    // Not fused, poll global
                    msgRaw = redisTemplate.execute(pollScript,
                            Arrays.asList(GLOBAL_QUEUE_HIGH, GLOBAL_QUEUE_NORMAL),
                            currentNodeId, String.valueOf(System.currentTimeMillis()), META_PREFIX, PROCESSING_PREFIX);
                }

                if (msgRaw == null) {
                    // Empty queue, sleep to avoid spin
                    Thread.sleep(200);
                    continue;
                }

                // Parse: <executionId>|<priority>\n<json>
                int p = msgRaw.indexOf('\n');
                if (p < 0) continue; // Should be handled by Lua (dropped to dead queue)

                String header = msgRaw.substring(0, p);
                String json = msgRaw.substring(p + 1);

                final String finalMsgRaw = msgRaw; // for ACK/Requeue

                msg = JSON.parseObject(json, TaskMessage.class);
                msg.setOriginalJson(finalMsgRaw); // Store full raw format for consistent remove

                // Double check executionId from header matches payload
                String execIdFromHeader = header.contains("|") ? header.substring(0, header.indexOf('|')) : header;
                if (msg.getExecutionId() == null) {
                    msg.setExecutionId(execIdFromHeader);
                }

                submitTask(msg, handler);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[RMQ_LISTEN_ERR] {}", e.getMessage());
                // In case of error (e.g. parse error), if we have msg we might need to ACK or DLQ manually?
                // For now, pollScript moves it to processing queue. If we crash here, Reclaim will handle it.
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void submitTask(TaskMessage msg, MessageHandler handler) {
        boolean submitted = false;
        while (!submitted && running && !Thread.currentThread().isInterrupted()) {
            try {
                consumerExecutor.execute(() -> {
                    try {
                        handler.handle(msg);
                        acknowledge(msg);
                    } catch (Exception e) {
                        requeue(msg, e);
                    }
                });
                submitted = true;
            } catch (RejectedExecutionException re) {
                // Backpressure: Sleep and retry
                try {
                    long sleep = quartzProperties.getBackpressureSleepMs();
                    Thread.sleep(sleep > 0 ? sleep : 50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void stopListening() {
        this.running = false;
    }

    /**
     * ACK: Remove from local processing queue and clear metadata
     */
    public void acknowledge(TaskMessage msg) {
        try {
            String processingKey = PROCESSING_PREFIX + currentNodeId;
            // Remove raw message
            redisTemplate.opsForList().remove(processingKey, 1, msg.getOriginalJson());
            // Remove metadata
            String execId = msg.getExecutionId();
            if (StringUtils.isNotEmpty(execId)) {
                redisTemplate.delete(META_PREFIX + execId);
            }
        } catch (Exception e) {
            log.warn("[RMQ_ACK_ERR] executionId={} {}", msg.getExecutionId(), e.getMessage());
        }
    }

    /**
     * Requeue: Remove from processing, update retry, delay push back
     */
    private void requeue(TaskMessage msg, Exception e) {
        try {
            String processingKey = PROCESSING_PREFIX + currentNodeId;
            redisTemplate.opsForList().remove(processingKey, 1, msg.getOriginalJson());

            String execId = msg.getExecutionId();
            if (StringUtils.isNotEmpty(execId)) {
                // Delete meta so it can be picked up again cleanly later
                redisTemplate.delete(META_PREFIX + execId);
            }

            // Retry Logic
            int retry = msg.getRetryCount(); // Current Node Retries
            int maxNodeRetries = quartzProperties.getMaxNodeRetries();

            // Attempt = retry + 1 (since this is called after failure)
            // If retry < maxNodeRetries - 1 (e.g. 0 < 2, 1 < 2), then increment and local retry.
            // If retry == 2 (meaning we just failed the 3rd time total), then trigger fuse.
            // Actually, simplified: if (retry + 1 < maxNodeRetries) => LOCAL
            // Example: max=3.
            // Initial (retry=0) -> Fail -> 0+1=1 < 3 -> Local Retry 1.
            // Retry 1 (retry=1) -> Fail -> 1+1=2 < 3 -> Local Retry 2.
            // Retry 2 (retry=2) -> Fail -> 2+1=3 (Not < 3) -> Fuse.
            // Result: 3 Execution attempts (Initial, Retry1, Retry2). Correct.

            if (retry + 1 < maxNodeRetries) {
                // === LOCAL RETRY ===
                msg.setRetryCount(retry + 1);
                long now = System.currentTimeMillis();
                long nextAt = now + quartzProperties.getNodeRetryIntervalMs();
                msg.setScheduledAt(nextAt);

                // Re-construct raw format
                String priority = StringUtils.isEmpty(msg.getPriority()) ? "NORMAL" : msg.getPriority();
                String newJson = JSON.toJSONString(msg);
                String newRaw = execId + "|" + priority + "\n" + newJson;
                msg.setOriginalJson(newRaw);

                redisTemplate.opsForZSet().add(LOCAL_RETRY_PREFIX + currentNodeId, newRaw, nextAt);

                log.warn("[RMQ_LOCAL_RETRY] executionId={} nodeRetry={} nextAt={} err={}",
                        execId, msg.getRetryCount(), nextAt, e.getMessage());

            } else {
                // === CIRCUIT BREAK (FUSE) ===
                // 1. Set Fuse Flag
                long fuseDuration = quartzProperties.getFuseDurationMs();
                String fuseKey = NODE_FUSED_PREFIX + currentNodeId;
                redisTemplate.opsForValue().set(fuseKey, "FUSED", fuseDuration, TimeUnit.MILLISECONDS);

                // 2. Reset Retry for Migration
                msg.setRetryCount(0);
                msg.setTotalFailureCount(msg.getTotalFailureCount() + 1);

                // 3. Move to Global Queue (Immediate)
                String priority = StringUtils.isEmpty(msg.getPriority()) ? "NORMAL" : msg.getPriority();
                String newJson = JSON.toJSONString(msg);
                String newRaw = execId + "|" + priority + "\n" + newJson;
                msg.setOriginalJson(newRaw);

                if ("HIGH".equals(priority)) {
                    redisTemplate.opsForList().leftPush(GLOBAL_QUEUE_HIGH, newRaw);
                } else {
                    redisTemplate.opsForList().leftPush(GLOBAL_QUEUE_NORMAL, newRaw);
                }

                log.error("[RMQ_NODE_FUSED] Node {} FUSED due to task {}. Migrated to Global. err={}",
                        currentNodeId, execId, e.getMessage());
            }

        } catch (Exception ex) {
            log.error("[RMQ_REQUEUE_ERR] executionId={}", msg.getExecutionId(), ex);
        }
    }

    /**
     * Enqueue Now (Compatible signature, ignores targetNode)
     */
    public void enqueueNow(SysJob sysJob, String targetNode, String priority) {
        enqueueAt(sysJob, targetNode, priority, System.currentTimeMillis());
    }

    /**
     * Enqueue At (Global, Competitive)
     * <p>Note: targetNode is ignored in competitive model.
     */
    public void enqueueAt(SysJob sysJob, String targetNode, String priority, long scheduledAtMillis) {
         // Delegate to Pipeline version but run in immediate mode
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            enqueueInPipeline(connection, sysJob, sysJob.getTraceId(), priority, scheduledAtMillis);
            return null;
        });
    }

    /**
     * Enqueue inside a Pipeline (Connection based)
     */
    public void enqueueInPipeline(org.springframework.data.redis.connection.RedisConnection connection,
                                  SysJob sysJob, String traceId, String priority, long scheduledAtMillis) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(String.valueOf(sysJob.getJobId()));

        String execId = traceId;
        if (StringUtils.isEmpty(execId)) {
            execId = UUID.randomUUID().toString();
        }
        msg.setExecutionId(execId);
        msg.setMessageType(TaskMessage.TYPE_EXECUTE);
        msg.setJobData(sysJob);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setTraceId(traceId);
        msg.setPriority(StringUtils.isEmpty(priority) ? "NORMAL" : priority);
        msg.setRetryCount(0);
        msg.setScheduledAt(scheduledAtMillis);

        // Format: <executionId>|<priority>\n<json>
        String json = JSON.toJSONString(msg);
        String rawMsg = execId + "|" + msg.getPriority() + "\n" + json;
        msg.setOriginalJson(rawMsg);

        byte[] rawBytes = rawMsg.getBytes();

        long now = System.currentTimeMillis();
        if (scheduledAtMillis <= now) {
            if ("HIGH".equalsIgnoreCase(priority)) {
                connection.lPush(GLOBAL_QUEUE_HIGH.getBytes(), rawBytes);
            } else {
                connection.lPush(GLOBAL_QUEUE_NORMAL.getBytes(), rawBytes);
            }
        } else {
            connection.zAdd(GLOBAL_DELAY_ZSET.getBytes(), scheduledAtMillis, rawBytes);
        }
    }

    /**
     * Promote Delayed Messages (Global)
     */
    private void promoteDueDelayedMessages() {
        long now = System.currentTimeMillis();
        int batch = 50;

        // Use Lua for atomicity
        Long count = redisTemplate.execute(promoteScript,
                Arrays.asList(GLOBAL_DELAY_ZSET, GLOBAL_QUEUE_HIGH, GLOBAL_QUEUE_NORMAL),
                String.valueOf(now), String.valueOf(batch));

        if (count != null && count > 0) {
            log.debug("[RMQ_PROMOTE] Promoted {} tasks", count);
        }
    }

    /**
     * Reclaim Processing Timeout (Global Scan)
     * Scans all active nodes' processing queues for stuck tasks.
     */
    private void reclaimGlobalProcessingTimeout() {
        // 1. Get all known nodes
        Set<String> members = redisTemplate.opsForSet().members("mq:nodes:alive");
        if (members == null) members = Collections.emptySet();
        Set<String> nodes = new HashSet<>(members);

        // Add current node explicitly just in case
        if (currentNodeId != null) nodes.add(currentNodeId);

        long now = System.currentTimeMillis();
        long timeoutMs = 60_000L; // 60s timeout

        for (String nodeIp : nodes) {
            String processingKey = PROCESSING_PREFIX + nodeIp;
            reclaimFromNode(processingKey, now, timeoutMs);
        }
    }

    private void reclaimFromNode(String processingKey, long now, long timeoutMs) {
        // Only peek top items (oldest) to avoid blocking/scanning too much
        List<String> items = redisTemplate.opsForList().range(processingKey, -50, -1);
        if (items == null || items.isEmpty()) return;

        for (String rawMsg : items) {
            try {
                int p = rawMsg.indexOf('\n');
                if (p < 0) continue;
                String header = rawMsg.substring(0, p);
                String execId = header.contains("|") ? header.substring(0, header.indexOf('|')) : header;

                // Check Meta
                String metaKey = META_PREFIX + execId;
                Object processingAtObj = redisTemplate.opsForHash().get(metaKey, "processingAt");

                boolean needReclaim = false;
                if (processingAtObj == null) {
                    // Meta missing implies inconsistent state (maybe failed ACK or lost meta), treat as reclaimable
                    needReclaim = true;
                } else {
                    long processingAt = Long.parseLong(processingAtObj.toString());
                    if (now - processingAt > timeoutMs) {
                        needReclaim = true;
                    }
                }

                if (needReclaim) {
                    // Try acquire lock to reclaim to avoid contention
                    String lockKey = RECLAIM_LOCK_PREFIX + execId;
                    Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

                    if (locked != null && locked) {
                        // Determine Target Queue from header
                        String priority = "NORMAL";
                        if (header.contains("|")) {
                             String pri = header.substring(header.indexOf('|') + 1);
                             if ("HIGH".equals(pri)) priority = "HIGH";
                        }
                        String targetQueue = "HIGH".equals(priority) ? GLOBAL_QUEUE_HIGH : GLOBAL_QUEUE_NORMAL;

                        // Execute Atomic Reclaim Script
                        Long result = redisTemplate.execute(reclaimScript,
                                Arrays.asList(processingKey, metaKey, targetQueue),
                                rawMsg);

                        if (result != null && result > 0) {
                            log.warn("[RMQ_RECLAIM] Reclaimed stuck task: {} from node={}", execId, processingKey);
                        }

                        // Clean lock
                        redisTemplate.delete(lockKey);
                    }
                }
            } catch (Exception e) {
                // ignore one bad item to continue scanning others
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

    public interface MessageHandler {
        void handle(TaskMessage message) throws Exception;
    }

    public static class TaskMessage {
        public static final String TYPE_EXECUTE = "EXECUTE";

        private String taskId;
        private String executionId;
        private String targetNode;
        private Object jobData;
        private long timestamp;
        private String messageType = TYPE_EXECUTE;
        private long scheduledAt;
        private Map<String, Object> payload;
        private String traceId;
        private int retryCount = 0; // Current Node Retry Count
        private int totalFailureCount = 0; // Global Failure Count
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

        public int getTotalFailureCount() { return totalFailureCount; }
        public void setTotalFailureCount(int totalFailureCount) { this.totalFailureCount = totalFailureCount; }

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
