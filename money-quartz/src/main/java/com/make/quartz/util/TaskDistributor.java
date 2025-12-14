package com.make.quartz.util;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.make.common.utils.StringUtils;
import com.make.quartz.config.IpBlackListManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 任务分发器
 * 负责决定任务在哪个节点执行，实现负载均衡和任务调度
 */
@Component
public class TaskDistributor {

    private static final Logger log = LoggerFactory.getLogger(TaskDistributor.class);

    /**
     * Redis key定义
     */
    private static final String SCHEDULER_NODES_KEY = "SCHEDULER_NODES";
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:";
    private static final String SCHEDULER_NODE_USAGE_SUFFIX = ":USAGE";
    private static final String SCHEDULER_NODE_HEARTBEAT_SUFFIX = ":HEARTBEAT";
    private static final String SCHEDULER_TASK_QUEUE = "SCHEDULER_TASK_QUEUE";
    private static final String SCHEDULER_LOCK_PREFIX = "SCHEDULER_LOCK:";
    private static final String SCHEDULER_MASTER_KEY = "SCHEDULER_MASTER";
    private static final String SCHEDULER_NODE_POOL_USAGE = "SCHEDULER_NODE_POOL_USAGE:";

    /**
     * 节点失联判定时间（毫秒），超过这个时间认为节点已失联
     */
    private static final long NODE_OFFLINE_THRESHOLD = 120000; // 2分钟

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IpBlackListManager ipBlackListManager;

    // 添加线程池用于异步任务分发
    private ThreadPoolExecutor taskDistributorExecutor;

    /**
     * 用于跟踪已分发但尚未执行完成的任务
     * key: taskId, value: 分发时间戳
     */
    private static final ConcurrentHashMap<String, Long> distributedTasks = new ConcurrentHashMap<>();

    public TaskDistributor() {
        // 初始化任务分发线程池
        this.taskDistributorExecutor = new ThreadPoolExecutor(
                2,
                100,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                r -> new Thread(r, "TaskDistributor-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 判断任务是否应该在当前节点执行
     *
     * @param taskId 任务ID
     * @param loadThreshold 负载阈值，超过此值考虑分发到其他节点
     * @return true-应该在当前节点执行，false-应该分发到其他节点
     */
    public boolean shouldExecuteLocally(String taskId, double loadThreshold) {
        try {
            // 检查任务是否已经分发但尚未完成
            if (distributedTasks.containsKey(taskId)) {
                log.info("任务 {} 已经分发但尚未完成，当前节点不执行", taskId);
                return false;
            }
            
            // 检查任务是否正在Redis消息队列中处理
            if (RedisMessageQueue.isMessageProcessing(taskId)) {
                log.info("任务 {} 正在Redis消息队列中处理，当前节点不执行", taskId);
                return false;
            }

            // 获取当前节点ID
            String currentNodeId = NodeRegistry.getCurrentNodeId();

            // 检查当前节点IP是否在黑名单中
            if (ipBlackListManager.isCurrentNodeIpBlacklisted()) {
                log.info("当前节点IP {} 在黑名单中，不能执行任务 {}",
                        ipBlackListManager.getCurrentNodeIp(), taskId);
                return false;
            }

            // 检查是否有任务锁，如果有且不是当前节点持有，则不执行
            String taskLockKey = SCHEDULER_LOCK_PREFIX + taskId;
            String lockOwner = redisTemplate.opsForValue().get(taskLockKey);
            if (lockOwner != null && !lockOwner.equals(currentNodeId)) {
                log.info("任务 {} 已被节点 {} 锁定，当前节点 {} 不执行", taskId, lockOwner, currentNodeId);
                return false;
            }

            // 获取当前节点负载
            String usageKey = SCHEDULER_NODE_PREFIX + currentNodeId + SCHEDULER_NODE_USAGE_SUFFIX;
            String usageStr = redisTemplate.opsForValue().get(usageKey);
            double currentNodeUsage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

            log.info("当前节点 {} 负载: {}", currentNodeId, currentNodeUsage);

            // 如果当前节点负载低于阈值，直接在当前节点执行
            if (currentNodeUsage < loadThreshold) {
                // 获取任务锁，确保只有一个节点执行任务
                if (acquireTaskLock(taskId, currentNodeId)) {
                    log.info("当前节点 {} 负载较低，任务 {} 在当前节点执行", currentNodeId, taskId);
                    return true;
                } else {
                    log.warn("无法获取任务 {} 锁，任务将分发到其他节点", taskId);
                    return false;
                }
            }

            // 当前节点负载较高，寻找负载最低的节点
            String targetNode = findLowestLoadNode(currentNodeId);
            if (targetNode == null) {
                // 没有找到其他可用节点，在当前节点执行
                if (acquireTaskLock(taskId, currentNodeId)) {
                    log.info("未找到其他可用节点，任务 {} 在当前节点 {} 执行", taskId, currentNodeId);
                    return true;
                } else {
                    log.warn("无法获取任务 {} 锁，任务执行失败", taskId);
                    return false;
                }
            }

            // 如果目标节点是当前节点
            if (targetNode.equals(currentNodeId)) {
                if (acquireTaskLock(taskId, currentNodeId)) {
                    log.info("当前节点 {} 是负载最低节点，任务 {} 在当前节点执行", currentNodeId, taskId);
                    return true;
                } else {
                    log.warn("无法获取任务 {} 锁，任务执行失败", taskId);
                    return false;
                }
            }

            // 将任务分发到目标节点
            if (distributeTaskToNode(taskId, targetNode)) {
                log.info("任务 {} 分发到节点 {} 执行", taskId, targetNode);
                // 标记任务已分发
                distributedTasks.put(taskId, System.currentTimeMillis());
                return false; // 当前节点不执行
            } else {
                // 分发失败，在当前节点执行
                log.warn("任务 {} 分发到节点 {} 失败，尝试在当前节点执行", taskId, targetNode);
                if (acquireTaskLock(taskId, currentNodeId)) {
                    log.warn("任务分发失败，任务 {} 在当前节点 {} 执行", taskId, currentNodeId);
                    return true;
                } else {
                    log.error("任务 {} 无法在任何节点执行", taskId);
                    // 记录到监控系统，标记为执行失败
                    recordFailedTask(taskId, "无法在任何节点执行");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("判断任务 {} 是否本地执行时发生异常", taskId, e);
            // 发生异常时，默认在当前节点执行，避免任务丢失
            log.warn("因异常默认在当前节点执行任务 {}", taskId);
            return true;
        }
    }

    /**
     * 记录执行失败的任务到监控系统
     * @param taskId 任务ID
     * @param reason 失败原因
     */
    private void recordFailedTask(String taskId, String reason) {
        try {
            // 这里可以添加具体的失败记录逻辑
            log.info("记录执行失败的任务: {}，原因: {}", taskId, reason);
        } catch (Exception e) {
            log.error("记录执行失败的任务时发生异常: {}", taskId, e);
        }
    }

    /**
     * 查找负载最低的节点
     *
     * @param excludeNode 需要排除的节点（通常是当前节点）
     * @return 负载最低的节点ID，如果没找到返回null
     */
    private String findLowestLoadNode(String excludeNode) {
        try {
            Set<String> nodes = redisTemplate.opsForSet().members(SCHEDULER_NODES_KEY);
            if (nodes == null || nodes.isEmpty()) {
                return null;
            }

            String lowestLoadNode = null;
            double lowestLoad = Double.MAX_VALUE;
            double highestLoad = Double.MIN_VALUE;

            long currentTime = System.currentTimeMillis();

            // 第一次遍历：找出最高和最低负载
            for (String node : nodes) {
                // 跳过排除的节点
                if (node.equals(excludeNode)) {
                    continue;
                }

                // 检查节点是否在线
                String heartbeatKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
                if (StringUtils.isEmpty(heartbeatStr)) {
                    continue; // 节点无心跳信息，认为已失联
                }

                long heartbeatTime = Long.parseLong(heartbeatStr);
                if (currentTime - heartbeatTime > NODE_OFFLINE_THRESHOLD) {
                    continue; // 节点心跳超时，认为已失联
                }

                // 检查节点IP是否在黑名单中
                String[] nodeParts = node.split(":");
                if (nodeParts.length >= 1) {
                    String nodeIp = nodeParts[0];
                    if (ipBlackListManager.isIpBlacklisted(nodeIp)) {
                        log.info("节点 {} 的IP {} 在黑名单中，跳过该节点", node, nodeIp);
                        continue;
                    }
                }

                // 获取节点负载
                String usageKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_USAGE_SUFFIX;
                String usageStr = redisTemplate.opsForValue().get(usageKey);
                double usage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

                // 更新最高和最低负载值
                if (usage > highestLoad) {
                    highestLoad = usage;
                }
                if (usage < lowestLoad) {
                    lowestLoad = usage;
                }
            }

            // 如果只有一个节点或所有节点负载相同，则直接返回最低负载节点
            if (lowestLoad == highestLoad) {
                for (String node : nodes) {
                    if (node.equals(excludeNode)) {
                        continue;
                    }

                    String heartbeatKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                    String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
                    if (StringUtils.isEmpty(heartbeatStr)) {
                        continue;
                    }

                    long heartbeatTime = Long.parseLong(heartbeatStr);
                    if (currentTime - heartbeatTime > NODE_OFFLINE_THRESHOLD) {
                        continue;
                    }

                    // 检查节点IP是否在黑名单中
                    String[] nodeParts = node.split(":");
                    if (nodeParts.length >= 1) {
                        String nodeIp = nodeParts[0];
                        if (ipBlackListManager.isIpBlacklisted(nodeIp)) {
                            log.info("节点 {} 的IP {} 在黑名单中，跳过该节点", node, nodeIp);
                            continue;
                        }
                    }

                    String usageKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_USAGE_SUFFIX;
                    String usageStr = redisTemplate.opsForValue().get(usageKey);
                    double usage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

                    if (usage == lowestLoad) {
                        return node;
                    }
                }
            }

            // 计算平均负载（排除最高负载节点）
            double totalLoad = 0.0;
            int validNodeCount = 0;
            String selectedNode = null;
            double minDifference = Double.MAX_VALUE;

            for (String node : nodes) {
                if (node.equals(excludeNode)) {
                    continue;
                }

                String heartbeatKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
                if (StringUtils.isEmpty(heartbeatStr)) {
                    continue;
                }

                long heartbeatTime = Long.parseLong(heartbeatStr);
                if (currentTime - heartbeatTime > NODE_OFFLINE_THRESHOLD) {
                    continue;
                }

                // 检查节点IP是否在黑名单中
                String[] nodeParts = node.split(":");
                if (nodeParts.length >= 1) {
                    String nodeIp = nodeParts[0];
                    if (ipBlackListManager.isIpBlacklisted(nodeIp)) {
                        log.info("节点 {} 的IP {} 在黑名单中，跳过该节点", node, nodeIp);
                        continue;
                    }
                }

                String usageKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_USAGE_SUFFIX;
                String usageStr = redisTemplate.opsForValue().get(usageKey);
                double usage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

                // 排除最高负载节点
                if (usage == highestLoad) {
                    continue;
                }

                totalLoad += usage;
                validNodeCount++;
            }

            // 如果没有有效的节点（除了最高负载节点外），则选择最低负载节点
            if (validNodeCount == 0) {
                for (String node : nodes) {
                    if (node.equals(excludeNode)) {
                        continue;
                    }

                    String heartbeatKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                    String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
                    if (StringUtils.isEmpty(heartbeatStr)) {
                        continue;
                    }

                    long heartbeatTime = Long.parseLong(heartbeatStr);
                    if (currentTime - heartbeatTime > NODE_OFFLINE_THRESHOLD) {
                        continue;
                    }

                    // 检查节点IP是否在黑名单中
                    String[] nodeParts = node.split(":");
                    if (nodeParts.length >= 1) {
                        String nodeIp = nodeParts[0];
                        if (ipBlackListManager.isIpBlacklisted(nodeIp)) {
                            log.info("节点 {} 的IP {} 在黑名单中，跳过该节点", node, nodeIp);
                            continue;
                        }
                    }

                    String usageKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_USAGE_SUFFIX;
                    String usageStr = redisTemplate.opsForValue().get(usageKey);
                    double usage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

                    if (usage == lowestLoad) {
                        return node;
                    }
                }
            }

            // 计算平均负载
            double averageLoad = totalLoad / validNodeCount;

            // 选择最接近平均负载的节点
            for (String node : nodes) {
                if (node.equals(excludeNode)) {
                    continue;
                }

                String heartbeatKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
                if (StringUtils.isEmpty(heartbeatStr)) {
                    continue;
                }

                long heartbeatTime = Long.parseLong(heartbeatStr);
                if (currentTime - heartbeatTime > NODE_OFFLINE_THRESHOLD) {
                    continue;
                }

                // 检查节点IP是否在黑名单中
                String[] nodeParts = node.split(":");
                if (nodeParts.length >= 1) {
                    String nodeIp = nodeParts[0];
                    if (ipBlackListManager.isIpBlacklisted(nodeIp)) {
                        log.info("节点 {} 的IP {} 在黑名单中，跳过该节点", node, nodeIp);
                        continue;
                    }
                }

                String usageKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_USAGE_SUFFIX;
                String usageStr = redisTemplate.opsForValue().get(usageKey);
                double usage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

                // 排除最高负载节点
                if (usage == highestLoad) {
                    continue;
                }

                double difference = Math.abs(usage - averageLoad);
                if (difference < minDifference) {
                    minDifference = difference;
                    selectedNode = node;
                }
            }

            return selectedNode;
        } catch (Exception e) {
            log.error("查找最低负载节点时发生异常", e);
            return null;
        }
    }

    /**
     * 获取任务执行锁
     *
     * @param taskId 任务ID
     * @param nodeId 节点ID
     * @return true-成功获取锁，false-获取锁失败
     */
    private boolean acquireTaskLock(String taskId, String nodeId) {
        try {
            String lockKey = SCHEDULER_LOCK_PREFIX + taskId;
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, nodeId, 60, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取任务 {} 锁失败", taskId, e);
            return false;
        }
    }

    /**
     * 尝试获取任务锁（用于抢占式任务分配）
     *
     * @param taskId 任务ID
     * @return true-成功获取锁，false-获取锁失败
     */
    public boolean tryAcquireTaskLock(String taskId) {
        String lockKey = SCHEDULER_LOCK_PREFIX + taskId;
        String lockValue = SchedulerManager.getCurrentNodeId();

        try {
            // 使用RedisTemplate实现分布式锁
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 60, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取任务锁失败，任务ID: {}", taskId, e);
            return false;
        }
    }

    /**
     * 将任务分发到指定节点
     *
     * @param taskId 任务ID
     * @param targetNode 目标节点ID
     * @return true-分发成功，false-分发失败
     */
    private boolean distributeTaskToNode(String taskId, String targetNode) {
        try {
            // 检查目标节点IP是否在黑名单中
            String[] nodeParts = targetNode.split(":");
            if (nodeParts.length >= 1) {
                String targetNodeIp = nodeParts[0];
                if (ipBlackListManager.isIpBlacklisted(targetNodeIp)) {
                    log.warn("目标节点 {} 的IP {} 在黑名单中，不能将任务 {} 分发到该节点",
                            targetNode, targetNodeIp, taskId);
                    return false;
                }
            }

            // 这里可以使用Redis的消息队列机制通知目标节点执行任务
            // 为简化实现，我们使用一个任务队列，目标节点定期轮询
            String taskQueueKey = SCHEDULER_NODE_PREFIX + targetNode + ":" + SCHEDULER_TASK_QUEUE;
            redisTemplate.opsForList().leftPush(taskQueueKey, taskId);
            redisTemplate.expire(taskQueueKey, 300, TimeUnit.SECONDS); // 5分钟过期

            // 更新任务锁信息，标记任务已被分发
            String lockKey = SCHEDULER_LOCK_PREFIX + taskId;
            redisTemplate.opsForValue().set(lockKey, targetNode, 60, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            log.error("将任务 {} 分发到节点 {} 时发生异常", taskId, targetNode, e);
            return false;
        }
    }

    /**
     * 释放任务执行锁
     *
     * @param taskId 任务ID
     */
    public void releaseTaskLock(String taskId) {
        try {
            String lockKey = SCHEDULER_LOCK_PREFIX + taskId;
            String lockOwner = redisTemplate.opsForValue().get(lockKey);

            if (lockOwner != null && lockOwner.equals(NodeRegistry.getCurrentNodeId())) {
                redisTemplate.delete(lockKey);
                log.info("任务 {} 锁已释放", taskId);
                // 从已分发任务列表中移除
                distributedTasks.remove(taskId);
            }
        } catch (Exception e) {
            log.error("释放任务 {} 锁时发生异常", taskId, e);
        }
    }

    /**
     * 异步检查是否有分发到当前节点的任务需要执行
     *
     * @param taskHandler 任务处理器
     */
    public void checkDistributedTasks(TaskHandler taskHandler) {
        // 检查当前节点IP是否在黑名单中
        if (ipBlackListManager.isCurrentNodeIpBlacklisted()) {
            log.info("当前节点IP {} 在黑名单中，不能消费分发的任务",
                    ipBlackListManager.getCurrentNodeIp());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String currentNodeId = NodeRegistry.getCurrentNodeId();
                String taskQueueKey = SCHEDULER_NODE_PREFIX + currentNodeId + ":" + SCHEDULER_TASK_QUEUE;

                // 尝试从队列中获取任务
                String taskId = redisTemplate.opsForList().rightPop(taskQueueKey, 1, TimeUnit.SECONDS);
                if (taskId != null) {
                    log.info("从队列中获取到分发任务: {}", taskId);

                    // 获取任务锁
                    if (acquireTaskLock(taskId, currentNodeId)) {
                        // 执行任务
                        taskHandler.handleTask(taskId);

                        // 释放任务锁
                        releaseTaskLock(taskId);
                    } else {
                        log.warn("无法获取分发任务 {} 的锁", taskId);
                    }
                }
            } catch (Exception e) {
                log.error("检查分发任务时发生异常", e);
            }
        }, taskDistributorExecutor);
    }

    /**
     * 任务处理器接口
     */
    @FunctionalInterface
    public interface TaskHandler {
        void handleTask(String taskId);
    }
    
    /**
     * 检查任务是否已分发但尚未完成
     * @param taskId 任务ID
     * @return true-已分发但未完成，false-未分发或已完成
     */
    public static boolean isTaskDistributed(String taskId) {
        return distributedTasks.containsKey(taskId);
    }
}