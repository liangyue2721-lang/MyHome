package com.make.quartz.util;

import com.make.common.utils.StringUtils;
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
     * Redis key定义 - 统一对齐 NodeRegistry
     */
    private static final String SCHEDULER_NODES_KEY = "mq:nodes:alive";
    private static final String NODE_TTL_PREFIX = "mq:nodes:ttl:";
    private static final String PROCESSING_QUEUE_PREFIX = "mq:task:processing:";

    // Global Queues from RedisMessageQueue
    private static final String GLOBAL_QUEUE_HIGH = "mq:task:global:high";
    private static final String GLOBAL_QUEUE_NORMAL = "mq:task:global:normal";
    
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
                    Thread.sleep(10000); // Check every 10s
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
            
            // 检查各节点状态
            for (String node : nodes) {
                // Check Liveness via TTL Key existence
                // NodeRegistry maintains this key with 30s TTL
                Boolean hasKey = redisTemplate.hasKey(NODE_TTL_PREFIX + node);
                
                if (Boolean.FALSE.equals(hasKey)) {
                    log.warn("[NODE_OFFLINE] Node {} TTL key missing, considering offline.", node);
                    handleOfflineNode(node);
                } else {
                    log.debug("Node {} is alive", node);
                }
            }
            
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
            
            // 从节点集合中移除
            redisTemplate.opsForSet().remove(SCHEDULER_NODES_KEY, nodeId);
            
            // 清理可能残留的 TTL Key
            redisTemplate.delete(NODE_TTL_PREFIX + nodeId);

            // 处理该节点的任务队列
            String taskQueueKey = PROCESSING_QUEUE_PREFIX + nodeId;

            long count = 0;
            while (true) {
                // RightPop - Take from tail
                String rawTask = redisTemplate.opsForList().rightPop(taskQueueKey);
                if (rawTask == null) {
                    break;
                }
                redistributeTask(rawTask);
                count++;
            }
            
            // 删除任务队列 (Should be empty now)
            redisTemplate.delete(taskQueueKey);
            
            log.info("失联节点 {} 处理完成. Migrated {} tasks.", nodeId, count);
        } catch (Exception e) {
            log.error("处理失联节点 {} 时发生异常", nodeId, e);
        }
    }
    
    /**
     * 重新分配任务到全局队列
     * 
     * @param rawTask 原始任务消息 (<executionId>|<priority>\n<json>)
     */
    private void redistributeTask(String rawTask) {
        try {
            if (StringUtils.isEmpty(rawTask)) return;

            // Extract Priority from Header
            String priority = "NORMAL";
            int p = rawTask.indexOf('\n');
            if (p > 0) {
                String header = rawTask.substring(0, p);
                if (header.contains("|")) {
                    String[] parts = header.split("\\|");
                    if (parts.length > 1) {
                         String pri = parts[1];
                         if ("HIGH".equalsIgnoreCase(pri)) {
                             priority = "HIGH";
                         }
                    }
                }
            }

            // Push back to Global Queue
            String headerLog = (p > 0) ? rawTask.substring(0, p) : "UNKNOWN_HEADER";
            if ("HIGH".equals(priority)) {
                redisTemplate.opsForList().leftPush(GLOBAL_QUEUE_HIGH, rawTask);
                log.info("[TASK_MIGRATE] Task migrated to HIGH queue. Header={}", headerLog);
            } else {
                redisTemplate.opsForList().leftPush(GLOBAL_QUEUE_NORMAL, rawTask);
                log.info("[TASK_MIGRATE] Task migrated to NORMAL queue. Header={}", headerLog);
            }

        } catch (Exception e) {
            log.error("重新分配任务时发生异常. RawTask len={}", rawTask.length(), e);
            // Fallback: push to dead letter? For now just log error.
        }
    }
}
