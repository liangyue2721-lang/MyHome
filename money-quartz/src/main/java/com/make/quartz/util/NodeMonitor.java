package com.make.quartz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 节点监控器
 * 负责监控集群中各节点状态，处理节点失联后的任务迁移等故障转移操作
 */
@Component
public class NodeMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(NodeMonitor.class);
    
    /**
     * Redis key定义
     */
    private static final String SCHEDULER_NODES_KEY = "SCHEDULER_NODES";
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:";
    private static final String SCHEDULER_NODE_HEARTBEAT_SUFFIX = ":HEARTBEAT";
    private static final String SCHEDULER_TASK_QUEUE = "SCHEDULER_TASK_QUEUE";
    
    /**
     * 节点失联判定时间（毫秒）
     */
    private static final long NODE_OFFLINE_THRESHOLD = 120000; // 2分钟
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private volatile boolean running = true;
    private Thread monitorThread;

    @PostConstruct
    public void init() {
        monitorThread = new Thread(() -> {
            log.info("启动节点监控线程");
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    checkNodeStatus();
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    log.info("节点监控线程被中断");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("节点监控线程异常", e);
                    // 避免死循环狂刷日志
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            log.info("节点监控线程已停止");
        }, "NodeMonitor-Thread");

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }
    
    /**
     * 定时检查节点状态
     * 每30秒执行一次
     */
    public void checkNodeStatus() {
        try {
            log.debug("开始检查节点状态");
            
            // 获取所有注册的节点
            Set<String> nodes = redisTemplate.opsForSet().members(SCHEDULER_NODES_KEY);
            if (nodes == null || nodes.isEmpty()) {
                log.debug("没有注册的节点");
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            boolean currentNodeIsMaster = false;
            
            // 检查各节点状态
            for (String node : nodes) {
                // 获取节点心跳时间
                String heartbeatKey = SCHEDULER_NODE_PREFIX + node + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
                String heartbeatStr = redisTemplate.opsForValue().get(heartbeatKey);
                
                if (heartbeatStr == null) {
                    log.warn("节点 {} 无心跳信息", node);
                    handleOfflineNode(node);
                    continue;
                }
                
                long heartbeatTime = Long.parseLong(heartbeatStr);
                if (currentTime - heartbeatTime > NODE_OFFLINE_THRESHOLD) {
                    log.warn("节点 {} 心跳超时，可能已失联", node);
                    handleOfflineNode(node);
                } else {
                    log.debug("节点 {} 状态正常", node);
                }
            }
            
            // 更新节点集合过期时间
            redisTemplate.expire(SCHEDULER_NODES_KEY, 300, TimeUnit.SECONDS);
            
            log.debug("节点状态检查完成");
        } catch (Exception e) {
            log.error("检查节点状态时发生异常", e);
        }
    }
    
    /**
     * 处理失联节点
     * 
     * @param nodeId 失联节点ID
     */
    private void handleOfflineNode(String nodeId) {
        try {
            log.info("开始处理失联节点: {}", nodeId);
            
            // 清理失联节点相关信息
            String heartbeatKey = SCHEDULER_NODE_PREFIX + nodeId + SCHEDULER_NODE_HEARTBEAT_SUFFIX;
            redisTemplate.delete(heartbeatKey);
            
            String usageKey = SCHEDULER_NODE_PREFIX + nodeId + ":USAGE";
            redisTemplate.delete(usageKey);
            
            // 从节点集合中移除
            redisTemplate.opsForSet().remove(SCHEDULER_NODES_KEY, nodeId);
            
            // 处理该节点的任务队列
            String taskQueueKey = SCHEDULER_NODE_PREFIX + nodeId + ":" + SCHEDULER_TASK_QUEUE;
            while (redisTemplate.opsForList().size(taskQueueKey) != null && 
                   redisTemplate.opsForList().size(taskQueueKey) > 0) {
                // 将任务迁移到其他节点
                String taskId = redisTemplate.opsForList().rightPop(taskQueueKey);
                if (taskId != null) {
                    redistributeTask(taskId);
                }
            }
            
            // 删除任务队列
            redisTemplate.delete(taskQueueKey);
            
            log.info("失联节点 {} 处理完成", nodeId);
        } catch (Exception e) {
            log.error("处理失联节点 {} 时发生异常", nodeId, e);
        }
    }
    
    /**
     * 重新分配任务到其他节点
     * 
     * @param taskId 任务ID
     */
    private void redistributeTask(String taskId) {
        try {
            log.info("重新分配任务: {}", taskId);
            
            // 获取当前所有在线节点
            Set<String> nodes = redisTemplate.opsForSet().members(SCHEDULER_NODES_KEY);
            if (nodes == null || nodes.isEmpty()) {
                log.warn("没有可用节点，任务 {} 暂时无法执行", taskId);
                return;
            }
            
            // 简单处理：将任务放入任务队列，让在线节点自行获取
            // 实际应用中可以实现更智能的负载均衡算法
            redisTemplate.opsForList().leftPush(SCHEDULER_TASK_QUEUE, taskId);
            redisTemplate.expire(SCHEDULER_TASK_QUEUE, 300, TimeUnit.SECONDS); // 5分钟过期
            
            log.info("任务 {} 已放入全局任务队列，等待节点获取", taskId);
        } catch (Exception e) {
            log.error("重新分配任务 {} 时发生异常", taskId, e);
        }
    }
}