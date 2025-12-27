package com.make.quartz.service.stock.queue;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.StockRefreshTask;
import com.make.quartz.domain.StockTaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
 */
@Service
public class StockTaskQueueService {

    private static final Logger log = LoggerFactory.getLogger(StockTaskQueueService.class);

    private static final String QUEUE_KEY = "mq:task:stock:refresh";
    private static final String LOCK_PREFIX = "stock:refresh:lock:";
    private static final String STATUS_KEY_PREFIX = "stock:refresh:status:"; // + stockCode + : + traceId
    private static final String STATUS_INDEX_KEY = "stock:refresh:status:index";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 投递任务
     */
    public void enqueue(StockRefreshTask task) {
        if (task == null) return;
        try {
            String json = JSON.toJSONString(task);
            redisTemplate.opsForList().leftPush(QUEUE_KEY, json);

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
     * 获取任务 (Blocking or simple pop)
     * 这里使用非阻塞 Pop，由消费者循环控制
     */
    public StockRefreshTask poll() {
        try {
            String json = redisTemplate.opsForList().rightPop(QUEUE_KEY);
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            return JSON.parseObject(json, StockRefreshTask.class);
        } catch (Exception e) {
            log.error("Failed to poll stock task", e);
            return null;
        }
    }

    /**
     * 尝试获取股票锁 (占位)
     * @param stockCode 股票代码
     * @param nodeId 节点ID (IP)
     * @return true if acquired
     */
    public boolean tryLockStock(String stockCode, String nodeId) {
        String key = LOCK_PREFIX + stockCode;
        // TTL 60s 防止死锁
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, nodeId, 60, TimeUnit.SECONDS);
        return success != null && success;
    }

    /**
     * 释放股票锁
     */
    public void releaseLock(String stockCode, String nodeId) {
        String key = LOCK_PREFIX + stockCode;
        try {
            String owner = redisTemplate.opsForValue().get(key);
            if (nodeId.equals(owner)) {
                redisTemplate.delete(key);
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
            redisTemplate.opsForValue().set(key, JSON.toJSONString(status), ttlSeconds, TimeUnit.SECONDS);

            // 2. Add to Index (Member: stockCode:traceId, Score: lastUpdateTime)
            String member = getIndexMember(stockCode, traceId);
            redisTemplate.opsForZSet().add(STATUS_INDEX_KEY, member, now);

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
                redisTemplate.delete(key);
                redisTemplate.opsForZSet().remove(STATUS_INDEX_KEY, member);
            }
        } catch (Exception e) {
            log.error("Failed to delete status for {}", stockCode, e);
        }
    }

    /**
     * 恢复 WAITING 状态的任务（系统启动时调用）
     * 防止任务在 Redis Queue 丢失但 Status 仍为 WAITING 的不一致情况。
     */
    public void recoverWaitingTasks() {
        try {
            log.info("Starting recovery of WAITING tasks...");
            List<StockTaskStatus> allStatuses = getAllStatuses();
            int recoveredCount = 0;

            for (StockTaskStatus status : allStatuses) {
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
            log.info("Recovered {} WAITING tasks into queue.", recoveredCount);
        } catch (Exception e) {
            log.error("Failed to recover waiting tasks", e);
        }
    }

    /**
     * 获取所有状态 (用于监控列表)
     * Strategy:
     * 1. Fetch all members from ZSet.
     * 2. Construct keys and MGET.
     * 3. Lazy clean missing keys from ZSet.
     */
    public List<StockTaskStatus> getAllStatuses() {
        List<StockTaskStatus> list = new ArrayList<>();
        try {
            // 1. Get all members from ZSet (ordered by time)
            Set<String> members = redisTemplate.opsForZSet().range(STATUS_INDEX_KEY, 0, -1);
            if (members == null || members.isEmpty()) {
                return list;
            }

            // 2. Construct Keys
            List<String> keys = new ArrayList<>();
            // Keep mapping of key -> member for cleanup
            List<String> memberList = new ArrayList<>(members);

            for (String member : memberList) {
                // member format: stockCode:traceId
                // key format: stock:refresh:status:stockCode:traceId
                // We can just replace the first colon? No, constructing carefully is safer.
                // Assuming member is stockCode:traceId, we can prepend prefix.
                // But stockCode *might* contain colons? Unlikely for stocks.
                // Let's rely on consistent construction.
                keys.add(STATUS_KEY_PREFIX + member);
            }

            // 3. MGET
            List<String> values = redisTemplate.opsForValue().multiGet(keys);

            // 4. Process Results & Lazy Cleanup
            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    String json = values.get(i);
                    if (StringUtils.isEmpty(json)) {
                        // Key expired or missing -> Clean from ZSet
                        String missingMember = memberList.get(i);
                        redisTemplate.opsForZSet().remove(STATUS_INDEX_KEY, missingMember);
                    } else {
                        list.add(JSON.parseObject(json, StockTaskStatus.class));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get all statuses", e);
        }
        return list;
    }

    private String getStatusKey(String stockCode, String traceId) {
        return STATUS_KEY_PREFIX + stockCode + ":" + traceId;
    }

    private String getIndexMember(String stockCode, String traceId) {
        return stockCode + ":" + traceId;
    }
}
