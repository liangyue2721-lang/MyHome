package com.make.quartz.util;

import com.make.common.utils.ip.IpUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 调度管理器
 * 负责主节点选举、线程池监控和任务分发
 */
@Component
public class SchedulerManager {
    
    private static final Logger log = LoggerFactory.getLogger(SchedulerManager.class);
    
    /**
     * Redis key定义
     */
    private static final String SCHEDULER_MASTER_KEY = "SCHEDULER_MASTER";
    private static final String SCHEDULER_NODES_KEY = "SCHEDULER_NODES";
    private static final String SCHEDULER_NODE_POOL_USAGE = "SCHEDULER_NODE_POOL_USAGE:";
    private static final String TASK_LOCK_PREFIX = "TASK_LOCK:";
    private static final String JOB_MASTER_NODE_PREFIX = "JOB_MASTER_NODE:";
    
    /** Redis消息队列相关key */
    private static final String TASK_QUEUE_PREFIX = "TASK_QUEUE:";
    private static final String TASK_CHANNEL = "TASK_CHANNEL";
    
    /**
     * 当前节点ID
     */
    private static final String CURRENT_NODE_ID = IpUtils.getHostIp() + ":" + UUID.randomUUID().toString();
    
    /**
     * 主节点选举锁
     */
    private static final String MASTER_ELECTION_LOCK = "MASTER_ELECTION_LOCK";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    
    @Autowired
    private RedissonClient redissonClient;
    
    @PostConstruct
    public void init() {
        try {
            // 初始化Redisson客户端
            log.info("SchedulerManager初始化完成，当前节点ID: {}", CURRENT_NODE_ID);
        } catch (Exception e) {
            log.error("SchedulerManager初始化RedissonClient失败", e);
        }
    }
    
    /**
     * 定时进行主节点选举
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void masterElection() {
        RLock lock = null;
        try {
            // 使用Redisson获取分布式锁，确保选举过程的互斥性
            lock = redissonClient.getLock(MASTER_ELECTION_LOCK);
            if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                // 检查是否已有主节点
                String currentMaster = redisTemplate.opsForValue().get(SCHEDULER_MASTER_KEY);
                if (currentMaster == null) {
                    // 没有主节点，当前节点成为主节点
                    redisTemplate.opsForValue().set(SCHEDULER_MASTER_KEY, CURRENT_NODE_ID, 60, TimeUnit.SECONDS);
                    log.info("当前节点 {} 成为主节点 (key was null)", CURRENT_NODE_ID);
                } else {
                    // 如果当前节点是主节点，则续期
                    if (CURRENT_NODE_ID.equals(currentMaster)) {
                        redisTemplate.expire(SCHEDULER_MASTER_KEY, 60, TimeUnit.SECONDS);
                        log.debug("主节点 {} 状态正常，已续期", currentMaster);
                    } else {
                        // 如果当前节点不是主节点，则什么都不做，等待主节点失效
                        // 注意：这里不能检查expireTime <= 0的情况，因为redisTemplate.opsForValue().get()在key过期时会返回null，已在上面处理
                        log.debug("当前节点 {} 不是主节点，主节点是: {}。跳过续期。", CURRENT_NODE_ID, currentMaster);
                    }
                }
            } else {
                log.debug("未能获取主节点选举锁，跳过本次选举");
            }
        } catch (Exception e) {
            log.error("主节点选举过程中发生异常", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 判断当前节点是否为主节点
     * 
     * @return true-是主节点，false-不是主节点
     */
    public boolean isMasterNode() {
        try {
            String masterNode = redisTemplate.opsForValue().get(SCHEDULER_MASTER_KEY);
            return CURRENT_NODE_ID.equals(masterNode);
        } catch (Exception e) {
            log.error("判断主节点状态时发生异常", e);
            return false;
        }
    }
    
    /**
     * 获取任务是否需要在主节点执行
     * 
     * @param jobId 任务ID
     * @return "1"-需要主节点执行，"0"-任意节点可执行
     */
    public String getJobIsMasterNode(Long jobId) {
        try {
            String key = JOB_MASTER_NODE_PREFIX + jobId;
            String isMasterNode = redisTemplate.opsForValue().get(key);
            return isMasterNode != null ? isMasterNode : "0";
        } catch (Exception e) {
            log.error("获取任务 {} 主节点执行要求时发生异常", jobId, e);
            return "0";
        }
    }
    
    /**
     * 设置任务是否需要在主节点执行
     * 
     * @param jobId 任务ID
     * @param isMasterNode "1"-需要主节点执行，"0"-任意节点可执行
     */
    public void setJobIsMasterNode(Long jobId, String isMasterNode) {
        // 参数检查
        if (jobId == null) {
            log.warn("任务ID为空，无法设置主节点执行要求");
            return;
        }
        
        if (isMasterNode == null) {
            isMasterNode = "0"; // 默认值
        }
        
        try {
            String key = JOB_MASTER_NODE_PREFIX + jobId;
            redisTemplate.opsForValue().set(key, isMasterNode);
            log.debug("设置任务 {} 是否需要主节点执行: {}", jobId, isMasterNode);
        } catch (Exception e) {
            log.error("设置任务是否需要主节点执行时发生异常", e);
        }
    }
    
    /**
     * 删除任务的主节点执行设置
     * 
     * @param jobId 任务ID
     */
    public void removeJobIsMasterNode(Long jobId) {
        // 参数检查
        if (jobId == null) {
            log.warn("任务ID为空，无法删除主节点执行设置");
            return;
        }
        
        try {
            String key = JOB_MASTER_NODE_PREFIX + jobId;
            redisTemplate.delete(key);
            log.debug("删除任务 {} 的主节点执行设置", jobId);
        } catch (Exception e) {
            log.error("删除任务的主节点执行设置时发生异常", e);
        }
    }
    
    /**
     * 获取当前节点ID
     * 
     * @return 当前节点ID
     */
    public static String getCurrentNodeId() {
        return CURRENT_NODE_ID;
    }
    
    /**
     * 更新线程池使用情况
     */
    @Scheduled(fixedRate = 10000)
    public void updateThreadPoolUsage() {
        try {
            // 计算线程池使用率
            int activeCount = threadPoolExecutor.getActiveCount();
            int poolSize = threadPoolExecutor.getPoolSize();
            double usage = poolSize > 0 ? (double) activeCount / poolSize : 0;
            
            // 上报到Redis
            String usageKey = SCHEDULER_NODE_POOL_USAGE + CURRENT_NODE_ID;
            redisTemplate.opsForValue().set(usageKey, String.valueOf(usage), 60, TimeUnit.SECONDS);
            
            log.debug("节点 {} 线程池使用率: {} (活动线程数: {}, 池大小: {})", 
                     CURRENT_NODE_ID, usage, activeCount, poolSize);
        } catch (Exception e) {
            log.error("更新线程池使用情况时发生异常", e);
        }
    }
    
    /**
     * 获取所有活跃节点列表
     * @return 节点ID列表
     */
    public java.util.List<String> getAliveNodes() {
        try {
            Set<String> nodes = redisTemplate.opsForSet().members(SCHEDULER_NODES_KEY);
            if (nodes == null || nodes.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            return new java.util.ArrayList<>(nodes);
        } catch (Exception e) {
            log.error("获取活跃节点失败", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 获取线程池使用率最低的节点
     * @param excludeCurrentNode 是否排除当前节点
     * @return 节点ID，如果没有可用节点则返回null
     */
    public String getLowestLoadNode(boolean excludeCurrentNode) {
        try {
            Set<String> nodes = redisTemplate.opsForSet().members(SCHEDULER_NODES_KEY);
            if (nodes == null || nodes.isEmpty()) {
                return null;
            }
            
            String currentNodeId = CURRENT_NODE_ID;
            
            String targetNode = null;
            double lowestUsage = Double.MAX_VALUE;
            
            for (String node : nodes) {
                // 如果需要排除当前节点且当前节点匹配，则跳过
                if (excludeCurrentNode && currentNodeId.equals(node)) {
                    continue;
                }
                
                // 获取节点的线程池使用率
                String usageStr = redisTemplate.opsForValue().get(SCHEDULER_NODE_POOL_USAGE + node);
                double usage = usageStr != null ? Double.parseDouble(usageStr) : 1.0;
                
                // 选择使用率最低的节点
                if (usage < lowestUsage) {
                    lowestUsage = usage;
                    targetNode = node;
                }
            }
            
            return targetNode;
        } catch (Exception e) {
            log.error("获取最低负载节点失败", e);
            return null;
        }
    }
    
    /**
     * 尝试获取任务锁
     * @param taskId 任务ID
     * @return true-成功获取锁，false-获取锁失败
     */
    public boolean tryAcquireTaskLock(String taskId) {
        try {
            String lockKey = TASK_LOCK_PREFIX + taskId;
            String lockValue = CURRENT_NODE_ID;
            
            // 使用RedisTemplate实现分布式锁
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 60, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取任务锁失败，任务ID: {}", taskId, e);
            return false;
        }
    }
    
    /**
     * 释放任务锁
     * @param taskId 任务ID
     */
    public void releaseTaskLock(String taskId) {
        try {
            String lockKey = TASK_LOCK_PREFIX + taskId;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("释放任务锁失败，任务ID: {}", taskId, e);
        }
    }
    
    /**
     * 从任务队列中获取任务
     * @return 任务ID，如果没有任务则返回null
     */
    public String processTaskFromQueue() {
        try {
            String queueKey = TASK_QUEUE_PREFIX + CURRENT_NODE_ID;
            String taskId = stringRedisTemplate.opsForList().rightPop(queueKey, 1, TimeUnit.SECONDS);
            
            if (taskId != null) {
                log.info("从消息队列获取到任务: {}", taskId);
                // 这里可以添加实际的任务处理逻辑
                return taskId;
            }
        } catch (Exception e) {
            log.error("处理消息队列任务失败", e);
        }
        return null;
    }
}