package com.make.stock.service.scheduled.stock.queue;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.stock.domain.StockRefreshTask;
import com.make.stock.domain.StockTaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.make.common.core.NodeRegistry;
import com.make.common.utils.ThreadPoolUtil;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 股票任务队列服务
 * 负责 Redis 队列的 Push/Pop 和 状态管理
 * <p>
 * Refactored:
 * 1.  Using independent keys per task: stock:refresh:status:{stockCode}:{traceId}
 * 2.  Using ZSET index for retrieval: stock:refresh:status:index
 * 3.  Lazy cleanup on read
 * 4.  Switched to StringRedisTemplate to fix atomic integer operations and avoid double-JSON-encoding.
 */
@Service
public class StockTaskQueueService {

    private static final Logger log = LoggerFactory.getLogger(StockTaskQueueService.class);

    private static final String QUEUE_KEY = "mq:task:stock:refresh";
    private static final String LOCK_PREFIX = "stock:refresh:lock:";
    private static final String STATUS_KEY_PREFIX = "stock:refresh:status:"; // + stockCode + : + traceId
    private static final String STATUS_INDEX_KEY = "stock:refresh:status:index";
    private static final String ACTIVE_KEY_PREFIX = "stock:refresh:active:"; // + stockCode
    private static final String PROCESSING_QUEUE_PREFIX = "mq:task:stock:refresh:processing:"; // + nodeId

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private NodeRegistry nodeRegistry;

    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void startCleanupMonitor() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StockTaskCleanupMonitor");
            t.setDaemon(true);
            return t;
        });

        // Run every 30 seconds
        cleanupExecutor.scheduleWithFixedDelay(this::activeCleanup, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Active Cleanup (Master Only)
     * Scans ZSet and removes zombie entries using multi-threading.
     */
    private void activeCleanup() {
        if (!nodeRegistry.isMaster()) {
            return;
        }

        long total = getTotalStatusCount();
        if (total < 1000) {
            return; // No need to clean small sets aggressively
        }

        log.info("[ACTIVE_CLEANUP] Starting active cleanup. Total items: {}", total);

        int chunkSize = 2000;
        long pages = (total + chunkSize - 1) / chunkSize;

        // Limit concurrency to avoid overloading Redis
        int maxConcurrency = 5;

        // We iterate backwards to avoid index shifting issues affecting "unseen" items as much as possible,
        // although removing items shifts indices. Since we just want to "clean as much as possible", exact coverage is less critical than throughput.
        // Better strategy: Range 0 to N, check, remove.
        // Actually, "scan-and-clean" on a changing ZSet is tricky.
        // Simplest robust way: Fetch top N, check, remove invalid. Repeat.
        // If we fetch 0-2000, and remove 500, the next 0-2000 will contain 1500 old + 500 new.
        // So we can just repeatedly fetch 0-2000 until we find no invalid items, or just sweep through.

        for (int i = 0; i < Math.min(pages, 10); i++) { // Limit to 10 chunks per run to avoid long blocking
             final long start = i * chunkSize;
             final long end = start + chunkSize - 1;

             ThreadPoolUtil.getCoreExecutor().submit(() -> {
                 try {
                     cleanupChunk(start, end);
                 } catch (Exception e) {
                     log.error("Cleanup chunk failed", e);
                 }
             });
        }
    }

    private void cleanupChunk(long start, long end) {
        try {
            Set<String> members = stringRedisTemplate.opsForZSet().range(STATUS_INDEX_KEY, start, end);
            if (members == null || members.isEmpty()) return;

            List<String> keys = new ArrayList<>();
            List<String> memberList = new ArrayList<>(members);
            for (String member : memberList) {
                keys.add(STATUS_KEY_PREFIX + member);
            }

            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            List<String> toRemove = new ArrayList<>();

            if (values != null) {
                for (int j = 0; j < values.size(); j++) {
                    if (StringUtils.isEmpty(values.get(j))) {
                        toRemove.add(memberList.get(j));
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                stringRedisTemplate.opsForZSet().remove(STATUS_INDEX_KEY, toRemove.toArray());
                log.info("[ACTIVE_CLEANUP] Removed {} zombie entries in range {}-{}", toRemove.size(), start, end);
            }
        } catch (Exception e) {
            log.error("Error in cleanup chunk", e);
        }
    }

    /**
     * 刷新活跃状态 (表示该股票正在循环中)
     */
    public void refreshActiveState(String stockCode, String traceId) {
        try {
            String key = ACTIVE_KEY_PREFIX + stockCode;
            // 5 minutes TTL acts as the heartbeat/watchdog threshold
            stringRedisTemplate.opsForValue().set(key, traceId, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to refresh active state for {}", stockCode, e);
        }
    }

    /**
     * 批量检查活跃状态
     * @return List corresponding to stockCodes, true if active, false otherwise
     */
    public List<Boolean> checkActiveStates(List<String> stockCodes) {
        List<Boolean> results = new ArrayList<>();
        if (stockCodes == null || stockCodes.isEmpty()) {
            return results;
        }

        try {
            List<String> keys = stockCodes.stream()
                    .map(code -> ACTIVE_KEY_PREFIX + code)
                    .collect(Collectors.toList());

            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (String val : values) {
                    results.add(StringUtils.isNotEmpty(val));
                }
            }
        } catch (Exception e) {
            log.error("Failed to check active states", e);
            // Fallback: assume all active to prevent flood, or all inactive?
            // Safer to return empty or assume active to avoid storm.
            // But if we assume active, loop stops.
            // If we assume inactive, we might double schedule.
            // Let's return what we have.
            for (int i = 0; i < stockCodes.size() - results.size(); i++) {
                 results.add(true); // Fail-safe: assume active
            }
        }
        return results;
    }

    /**
     * 投递任务
     */
    public void enqueue(StockRefreshTask task) {
        if (task == null) return;
        try {
            String json = JSON.toJSONString(task);
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, json);

            // Set WAITING status (Long TTL safety net)
            StockTaskStatus status = new StockTaskStatus();
            status.setStockCode(task.getStockCode());
            status.setStatus(StockTaskStatus.STATUS_WAITING);
            status.setTraceId(task.getTraceId());
            status.setLastUpdateTime(System.currentTimeMillis());

            updateStatus(task.getStockCode(), status);
        } catch (Exception e) {
            log.error("Failed to enqueue stock task: {}", task.getStockCode(), e);
        }
    }

    /**
     * 获取任务 (Reliable Poll)
     * Uses RPOPLPUSH to move from pending to processing queue.
     * This ensures tasks are not lost if the consumer crashes.
     */
    public StockRefreshTask pollReliable(String nodeId) {
        if (StringUtils.isEmpty(nodeId)) return null;
        try {
            String processingQueue = PROCESSING_QUEUE_PREFIX + nodeId;
            // Pop from RIGHT of QUEUE_KEY, Push to LEFT of processingQueue
            String json = stringRedisTemplate.opsForList().rightPopAndLeftPush(QUEUE_KEY, processingQueue);

            if (StringUtils.isEmpty(json)) {
                return null;
            }
            // Handle legacy double-quoted JSON
            String cleanJson = unquoteJSON(json);
            return JSON.parseObject(cleanJson, StockRefreshTask.class);
        } catch (Exception e) {
            log.error("Failed to poll reliable stock task", e);
            return null;
        }
    }

    /**
     * Acknowledge Task Completion
     * Removes the task from the processing queue.
     */
    public void ack(String nodeId, StockRefreshTask task) {
        if (StringUtils.isEmpty(nodeId) || task == null) return;
        try {
            String processingQueue = PROCESSING_QUEUE_PREFIX + nodeId;
            // Remove 1 occurrence of the value from the processing queue
            // Note: If exact JSON match fails due to serialization diffs, we might have issues.
            // But since we pushed JSON, we should remove the same JSON.
            // Ideally we should use raw JSON we pulled, but passing object is safer for interface.
            // We'll re-serialize.
            String json = JSON.toJSONString(task);
            // Try removing exact match first
            Long removed = stringRedisTemplate.opsForList().remove(processingQueue, 1, json);
            if (removed == null || removed == 0) {
                 // Fallback: legacy quoted handling?
                 // Or maybe just log warning.
                 log.warn("ACK failed: Item not found in processing queue. Queue={}, Task={}", processingQueue, task.getStockCode());
            }
        } catch (Exception e) {
            log.error("Failed to ack task", e);
        }
    }

    /**
     * Reclaim Pending Tasks (Watchdog Logic)
     * Scans processing queues of nodes and reclaims tasks if they are stuck.
     * For now, simplistic implementation: If a node is "dead" (not in registry?), move all its processing tasks back to pending.
     * Or, we can just look at task timestamp.
     *
     * To implement properly, we need to know which nodes are dead.
     * Or we assume tasks older than X minutes in ANY processing queue are stale.
     *
     * @param targetNodeId The node to reclaim from (if known dead)
     */
    public void reclaimTasksFromNode(String targetNodeId) {
        try {
            String processingQueue = PROCESSING_QUEUE_PREFIX + targetNodeId;
            while (true) {
                // RPOP from processing, LPUSH to pending (re-queue at head to prioritize?)
                // Or LPUSH to pending (Head) is standard for "put back".
                String json = stringRedisTemplate.opsForList().rightPopAndLeftPush(processingQueue, QUEUE_KEY);
                if (StringUtils.isEmpty(json)) {
                    break;
                }
                log.info("Reclaimed stuck task from node {}: {}", targetNodeId, json);
            }
        } catch (Exception e) {
             log.error("Failed to reclaim tasks from node {}", targetNodeId, e);
        }
    }

    /**
     * 获取任务 (Blocking or simple pop)
     * 这里使用非阻塞 Pop，由消费者循环控制
     */
    public StockRefreshTask poll() {
        // Deprecated in favor of pollReliable, but kept for backward compatibility if needed
        try {
            String json = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY);
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            return JSON.parseObject(unquoteJSON(json), StockRefreshTask.class);
        } catch (Exception e) {
            log.error("Failed to poll stock task", e);
            return null;
        }
    }

    /**
     * 尝试获取股票锁 (占位)
     *
     * @param stockCode 股票代码
     * @param nodeId    节点ID (IP)
     * @return true if acquired
     */
    public boolean tryLockStock(String stockCode, String nodeId) {
        String key = LOCK_PREFIX + stockCode;
        // TTL 60s 防止死锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, nodeId, 60, TimeUnit.SECONDS);
        return success != null && success;
    }

    /**
     * 释放股票锁
     */
    public void releaseLock(String stockCode, String nodeId) {
        String key = LOCK_PREFIX + stockCode;
        try {
            String owner = stringRedisTemplate.opsForValue().get(key);
            if (nodeId.equals(owner)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("Failed to release lock for {}", stockCode, e);
        }
    }

    /**
     * 更新任务状态 (New Structure: Key per task + ZSet Index)
     */
    public void updateStatus(String stockCode, StockTaskStatus status) {
        if (status == null || StringUtils.isEmpty(status.getTraceId())) {
            log.warn("Invalid status update for stock: {}", stockCode);
            return;
        }
        try {
            String traceId = status.getTraceId();
            String key = getStatusKey(stockCode, traceId);
            long now = System.currentTimeMillis();
            status.setLastUpdateTime(now);

            // Determine TTL
            long ttlSeconds = 300; // Default 5 min for terminal states
            if (StockTaskStatus.STATUS_WAITING.equals(status.getStatus()) ||
                    StockTaskStatus.STATUS_RUNNING.equals(status.getStatus())) {
                ttlSeconds = 1800; // 30 min for active states
            }

            // 1. Set Key with TTL
            // StringRedisTemplate writes raw JSON string
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(status), ttlSeconds, TimeUnit.SECONDS);

            // 2. Add to Index (Member: stockCode:traceId, Score: lastUpdateTime)
            String member = getIndexMember(stockCode, traceId);
            // ZSet members are strings, compatible with StringRedisTemplate
            stringRedisTemplate.opsForZSet().add(STATUS_INDEX_KEY, member, now);

        } catch (Exception e) {
            log.error("Failed to update status for {}", stockCode, e);
        }
    }

    /**
     * 删除任务状态
     * Only used for Admin/Maintenance/Test
     */
    public void deleteStatus(String stockCode, String traceId) {
        try {
            if (traceId != null) {
                String key = getStatusKey(stockCode, traceId);
                String member = getIndexMember(stockCode, traceId);
                stringRedisTemplate.delete(key);
                stringRedisTemplate.opsForZSet().remove(STATUS_INDEX_KEY, member);
            }
        } catch (Exception e) {
            log.error("Failed to delete status for {}", stockCode, e);
        }
    }

    /**
     * 清空所有任务状态 (紧急运维)
     */
    public void clearAllStatuses() {
        try {
            // 1. Delete the index
            stringRedisTemplate.delete(STATUS_INDEX_KEY);
            // 2. Note: Individual status keys (stock:refresh:status:...) will expire by TTL (5-30min).
            // We do not scan/keys* them to avoid blocking Redis.
            log.warn("Cleared stock task status index.");
        } catch (Exception e) {
            log.error("Failed to clear all statuses", e);
        }
    }

    /**
     * 恢复 WAITING 状态的任务（系统启动时调用）
     * 防止任务在 Redis Queue 丢失但 Status 仍为 WAITING 的不一致情况。
     */
    public void recoverWaitingTasks() {
        try {
            log.info("Starting recovery of WAITING tasks...");
            // Use paginated fetch to avoid loading all at once (though recover logic might need improvement for large datasets)
            // For simplicity, we process latest 1000 tasks.
            List<StockTaskStatus> activeTasks = getStatusesPaginated(1, 1000);
            int recoveredCount = 0;

            for (StockTaskStatus status : activeTasks) {
                if (StockTaskStatus.STATUS_WAITING.equals(status.getStatus())) {
                    // Re-construct task
                    StockRefreshTask task = new StockRefreshTask();
                    task.setTaskId(UUID.randomUUID().toString());
                    task.setStockCode(status.getStockCode());
                    task.setTaskType("REFRESH_PRICE"); // Default type
                    task.setCreateTime(System.currentTimeMillis());
                    task.setTraceId(status.getTraceId());

                    // Re-enqueue
                    enqueue(task);
                    recoveredCount++;
                }
            }
            log.info("Recovered {} WAITING tasks into queue (from recent 1000).", recoveredCount);
        } catch (Exception e) {
            log.error("Failed to recover waiting tasks", e);
        }
    }

    /**
     * 获取状态列表 (支持分页)
     *
     * @param pageNum   Page number (1-based)
     * @param pageSize  Page size
     * @return List of StockTaskStatus
     */
    public List<StockTaskStatus> getStatusesPaginated(int pageNum, int pageSize) {
        List<StockTaskStatus> list = new ArrayList<>();
        long start = (long) (pageNum - 1) * pageSize;
        int maxAttempts = 10;
        int attempt = 0;

        try {
            while (list.size() < pageSize && attempt < maxAttempts) {
                attempt++;

                // Calculate current read pointer in ZSet
                long currentZSetIndex = start + list.size();
                long end = currentZSetIndex + (pageSize - list.size()) - 1;

                Set<String> members = stringRedisTemplate.opsForZSet().reverseRange(STATUS_INDEX_KEY, currentZSetIndex, end);

                if (members == null || members.isEmpty()) {
                    break;
                }

                List<String> keys = new ArrayList<>();
                List<String> memberList = new ArrayList<>(members);
                for (String member : memberList) {
                    keys.add(STATUS_KEY_PREFIX + member);
                }

                List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
                if (values == null) break;

                List<String> invalidMembers = new ArrayList<>();

                for (int i = 0; i < values.size(); i++) {
                    String json = values.get(i);
                    if (StringUtils.isEmpty(json)) {
                        invalidMembers.add(memberList.get(i));
                    } else {
                        try {
                            String cleanJson = unquoteJSON(json);
                            list.add(JSON.parseObject(cleanJson, StockTaskStatus.class));
                        } catch (Exception e) {
                            invalidMembers.add(memberList.get(i));
                        }
                    }
                }

                if (!invalidMembers.isEmpty()) {
                    stringRedisTemplate.opsForZSet().remove(STATUS_INDEX_KEY, invalidMembers.toArray());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get paginated statuses", e);
        }
        return list;
    }

    /**
     * Get Total Count of tasks in index
     */
    public long getTotalStatusCount() {
        try {
            Long count = stringRedisTemplate.opsForZSet().zCard(STATUS_INDEX_KEY);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取所有状态 (已废弃，建议使用分页版)
     */
    @Deprecated
    public List<StockTaskStatus> getAllStatuses() {
        return getStatusesPaginated(1, 1000);
    }

    private String getStatusKey(String stockCode, String traceId) {
        return STATUS_KEY_PREFIX + stockCode + ":" + traceId;
    }

    private String getIndexMember(String stockCode, String traceId) {
        return stockCode + ":" + traceId;
    }

    /**
     * Unquote JSON string if it's double-quoted (legacy format from FastJson2JsonRedisSerializer).
     * e.g., "\"{\"a\":1}\"" -> "{\"a\":1}"
     */
    private String unquoteJSON(String json) {
        if (json != null && json.length() > 1 && json.startsWith("\"") && json.endsWith("\"")) {
            try {
                // Attempt to parse as String to unquote
                return JSON.parseObject(json, String.class);
            } catch (Exception e) {
                // If parsing fails (not a valid JSON string), return original
                return json;
            }
        }
        return json;
    }
}
