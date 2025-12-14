package com.make.framework.config;

import com.alibaba.fastjson2.JSON;
import com.make.common.core.redis.RedisCache;
import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis集群线程池信息服务
 * 用于在分布式环境中通过Redis收集和展示所有节点的线程池信息
 *
 * @author make
 */
@Service
public class RedisClusterThreadPoolService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisClusterThreadPoolService.class);
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ThreadPoolMonitor threadPoolMonitor;
    
    // 应用名称，用于区分不同服务
    @Value("${spring.application.name:make-money}")
    private String applicationName;
    
    // Redis中存储集群线程池信息的键名
    private static final String CLUSTER_THREAD_POOL_KEY = "cluster:thread:pool:info";
    
    // 集群节点信息过期时间（秒）
    private static final long NODE_EXPIRE_TIME = 60L;
    
    /**
     * 更新本节点线程池信息到Redis中
     */
    public void updateLocalNodeInfoToRedis() {
        try {
            // 获取本节点线程池信息
            Map<String, Object> localInfo = threadPoolMonitor.getLocalThreadPoolInfo();
            
            // 获取本节点标识（IP地址+应用名称）
            String localNodeId = IpUtils.getHostIp() + ":" + applicationName;
            
            // 将节点信息存储到Redis的Hash结构中，并设置过期时间
            redisCache.setCacheMapValue(CLUSTER_THREAD_POOL_KEY, localNodeId, JSON.toJSONString(localInfo));
            redisCache.expire(CLUSTER_THREAD_POOL_KEY, NODE_EXPIRE_TIME, TimeUnit.SECONDS);
            
            logger.info("✅ 更新本节点线程池信息到Redis中: 节点={}, 信息={}", localNodeId, localInfo);
        } catch (Exception e) {
            logger.error("💥 更新本节点线程池信息到Redis失败", e);
        }
    }
    
    /**
     * 从Redis中获取所有节点的线程池信息
     * 
     * @return 所有节点的线程池信息
     */
    public Map<String, Map<String, Object>> getAllNodeThreadPoolInfoFromRedis() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        try {
            // 从Redis中获取所有节点信息
            Map<String, String> nodeInfoMap = redisCache.getCacheMap(CLUSTER_THREAD_POOL_KEY);
            
            logger.debug("🔍 从Redis中获取节点信息，节点数量: {}", nodeInfoMap.size());
            
            // 反序列化节点信息
            for (Map.Entry<String, String> entry : nodeInfoMap.entrySet()) {
                String nodeId = entry.getKey();
                String nodeInfoJson = entry.getValue();
                
                // 过滤掉包含127.0.0.1的节点
                if (nodeId.contains("127.0.0.1")) {
                    logger.debug("🚫 过滤掉本地节点: {}", nodeId);
                    continue;
                }
                
                Map<String, Object> nodeInfo = JSON.parseObject(nodeInfoJson, Map.class);
                result.put(nodeId, nodeInfo);
                
                logger.debug("📄 节点{}信息: {}", nodeId, nodeInfoJson);
            }
            
            logger.info("📊 从Redis中获取集群线程池信息: 节点数量={}", result.size());
        } catch (Exception e) {
            logger.error("💥 从Redis中获取集群线程池信息失败", e);
        }
        
        return result;
    }
    
    /**
     * 从Redis中获取所有节点的线程池信息并计算聚合统计
     * 
     * @return 聚合统计信息
     */
    public Map<String, Object> getAggregatedThreadPoolInfoFromRedis() {
        Map<String, Object> aggregatedInfo = new HashMap<>();
        try {
            Map<String, Map<String, Object>> allNodesInfo = getAllNodeThreadPoolInfoFromRedis();
            
            // 初始化聚合统计值
            long totalCorePoolSize = 0;
            long totalMaximumPoolSize = 0;
            long totalActiveCount = 0;
            long totalPoolSize = 0;
            long totalCompletedTaskCount = 0;
            long totalQueueSize = 0;
            long totalQueueRemainingCapacity = 0;
            long totalTaskCount = 0;
            
            // 遍历所有节点信息进行聚合
            for (Map<String, Object> nodeInfo : allNodesInfo.values()) {
                // 处理核心业务线程池信息
                Map<String, Object> coreExecutor = (Map<String, Object>) nodeInfo.getOrDefault("coreExecutor", new HashMap<>());
                if (!coreExecutor.isEmpty()) {
                    totalCorePoolSize += ((Number) coreExecutor.getOrDefault("corePoolSize", 0)).intValue();
                    totalMaximumPoolSize += ((Number) coreExecutor.getOrDefault("maximumPoolSize", 0)).intValue();
                    totalActiveCount += ((Number) coreExecutor.getOrDefault("activeCount", 0)).intValue();
                    totalPoolSize += ((Number) coreExecutor.getOrDefault("poolSize", 0)).intValue();
                    totalCompletedTaskCount += ((Number) coreExecutor.getOrDefault("completedTaskCount", 0L)).longValue();
                    totalQueueSize += ((Number) coreExecutor.getOrDefault("queueSize", 0)).intValue();
                    totalQueueRemainingCapacity += ((Number) coreExecutor.getOrDefault("queueRemainingCapacity", 0)).intValue();
                    totalTaskCount += ((Number) coreExecutor.getOrDefault("taskCount", 0L)).longValue();
                }
                
                // 处理关注股票专用线程池信息
                Map<String, Object> watchStockExecutor = (Map<String, Object>) nodeInfo.getOrDefault("watchStockExecutor", new HashMap<>());
                if (!watchStockExecutor.isEmpty()) {
                    totalCorePoolSize += ((Number) watchStockExecutor.getOrDefault("corePoolSize", 0)).intValue();
                    totalMaximumPoolSize += ((Number) watchStockExecutor.getOrDefault("maximumPoolSize", 0)).intValue();
                    totalActiveCount += ((Number) watchStockExecutor.getOrDefault("activeCount", 0)).intValue();
                    totalPoolSize += ((Number) watchStockExecutor.getOrDefault("poolSize", 0)).intValue();
                    totalCompletedTaskCount += ((Number) watchStockExecutor.getOrDefault("completedTaskCount", 0L)).longValue();
                    totalQueueSize += ((Number) watchStockExecutor.getOrDefault("queueSize", 0)).intValue();
                    totalQueueRemainingCapacity += ((Number) watchStockExecutor.getOrDefault("queueRemainingCapacity", 0)).intValue();
                    totalTaskCount += ((Number) watchStockExecutor.getOrDefault("taskCount", 0L)).longValue();
                }
                
                // 处理调度线程池信息
                Map<String, Object> scheduler = (Map<String, Object>) nodeInfo.getOrDefault("scheduler", new HashMap<>());
                if (!scheduler.isEmpty()) {
                    totalCorePoolSize += ((Number) scheduler.getOrDefault("corePoolSize", 0)).intValue();
                    totalMaximumPoolSize += ((Number) scheduler.getOrDefault("maximumPoolSize", 0)).intValue();
                    totalActiveCount += ((Number) scheduler.getOrDefault("activeCount", 0)).intValue();
                    totalPoolSize += ((Number) scheduler.getOrDefault("poolSize", 0)).intValue();
                    totalCompletedTaskCount += ((Number) scheduler.getOrDefault("completedTaskCount", 0L)).longValue();
                    totalQueueSize += ((Number) scheduler.getOrDefault("queueSize", 0)).intValue();
                    totalQueueRemainingCapacity += ((Number) scheduler.getOrDefault("queueRemainingCapacity", 0)).intValue();
                    totalTaskCount += ((Number) scheduler.getOrDefault("taskCount", 0L)).longValue();
                }
            }
            
            // 构建聚合统计信息
            aggregatedInfo.put("nodeCount", allNodesInfo.size());
            aggregatedInfo.put("totalCorePoolSize", totalCorePoolSize);
            aggregatedInfo.put("totalMaximumPoolSize", totalMaximumPoolSize);
            aggregatedInfo.put("totalActiveCount", totalActiveCount);
            aggregatedInfo.put("totalPoolSize", totalPoolSize);
            aggregatedInfo.put("totalCompletedTaskCount", totalCompletedTaskCount);
            aggregatedInfo.put("totalQueueSize", totalQueueSize);
            aggregatedInfo.put("totalQueueRemainingCapacity", totalQueueRemainingCapacity);
            aggregatedInfo.put("totalTaskCount", totalTaskCount);
            
            logger.info("📈 从Redis中获取集群线程池聚合信息: 节点数量={}, 总活跃线程数={}", 
                    allNodesInfo.size(), totalActiveCount);
        } catch (Exception e) {
            logger.error("💥 从Redis中获取集群线程池聚合信息失败", e);
        }
        
        return aggregatedInfo;
    }
}