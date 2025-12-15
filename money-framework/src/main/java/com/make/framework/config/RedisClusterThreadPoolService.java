package com.make.framework.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.make.common.core.redis.RedisCache;
import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
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
            List<Map<String, Object>> localInfo = threadPoolMonitor.getLocalThreadPoolInfo();

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
    public Map<String, List<Map<String, Object>>> getAllNodeThreadPoolInfoFromRedis() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
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

                List<Map<String, Object>> nodeInfo = JSON.parseObject(nodeInfoJson, new TypeReference<List<Map<String, Object>>>() {});
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
            Map<String, List<Map<String, Object>>> allNodesInfo = getAllNodeThreadPoolInfoFromRedis();

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
            for (List<Map<String, Object>> nodeInfoList : allNodesInfo.values()) {
                for (Map<String, Object> poolInfo : nodeInfoList) {
                    totalCorePoolSize += ((Number) poolInfo.getOrDefault("corePoolSize", 0)).longValue();
                    totalMaximumPoolSize += ((Number) poolInfo.getOrDefault("maximumPoolSize", 0)).longValue();
                    totalActiveCount += ((Number) poolInfo.getOrDefault("activeCount", 0)).longValue();
                    totalPoolSize += ((Number) poolInfo.getOrDefault("poolSize", 0)).longValue();
                    totalCompletedTaskCount += ((Number) poolInfo.getOrDefault("completedTaskCount", 0L)).longValue();
                    totalQueueSize += ((Number) poolInfo.getOrDefault("queueSize", 0)).longValue();
                    totalQueueRemainingCapacity += ((Number) poolInfo.getOrDefault("queueRemainingCapacity", 0)).longValue();
                    totalTaskCount += ((Number) poolInfo.getOrDefault("taskCount", 0L)).longValue();
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