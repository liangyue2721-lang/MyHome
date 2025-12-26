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
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 股票任务队列服务
 * 负责 Redis 队列的 Push/Pop 和 状态管理
 */
@Service
public class StockTaskQueueService {

    private static final Logger log = LoggerFactory.getLogger(StockTaskQueueService.class);

    private static final String QUEUE_KEY = "mq:task:stock:refresh";
    private static final String LOCK_PREFIX = "stock:refresh:lock:";
    private static final String STATUS_HASH_KEY = "stock:refresh:status";

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
            // log.debug("Enqueue stock task: {}", task.getStockCode());
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
     * 更新任务状态 (Hash)
     */
    public void updateStatus(String stockCode, StockTaskStatus status) {
        try {
            redisTemplate.opsForHash().put(STATUS_HASH_KEY, stockCode, JSON.toJSONString(status));
        } catch (Exception e) {
            log.error("Failed to update status for {}", stockCode, e);
        }
    }

    /**
     * 删除任务状态 (Hash)
     */
    public void deleteStatus(String stockCode) {
        try {
            redisTemplate.opsForHash().delete(STATUS_HASH_KEY, stockCode);
        } catch (Exception e) {
            log.error("Failed to delete status for {}", stockCode, e);
        }
    }

    /**
     * 获取单个状态
     */
    public StockTaskStatus getStatus(String stockCode) {
        try {
            Object obj = redisTemplate.opsForHash().get(STATUS_HASH_KEY, stockCode);
            if (obj != null) {
                return JSON.parseObject(obj.toString(), StockTaskStatus.class);
            }
        } catch (Exception e) {
            log.error("Failed to get status for {}", stockCode, e);
        }
        return null;
    }

    /**
     * 获取所有状态 (用于监控列表)
     */
    public List<StockTaskStatus> getAllStatuses() {
        List<StockTaskStatus> list = new ArrayList<>();
        try {
            Map<Object, Object> map = redisTemplate.opsForHash().entries(STATUS_HASH_KEY);
            for (Object val : map.values()) {
                list.add(JSON.parseObject(val.toString(), StockTaskStatus.class));
            }
        } catch (Exception e) {
            log.error("Failed to get all statuses", e);
        }
        return list;
    }
}
