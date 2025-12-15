package com.make.framework.config;

import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.ip.IpUtils;
import com.make.framework.web.domain.server.ClusterThreadPoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控服务
 * 提供对系统中配置的线程池状态监控功能
 *
 * @author make
 */
@Component
public class ThreadPoolMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitor.class);

    @Autowired(required = false)
    private ClusterThreadPoolInfo clusterThreadPoolInfo;

    @Autowired(required = false)
    private RedisClusterThreadPoolService redisClusterThreadPoolService;

    // 应用名称，用于区分不同服务
    @Value("${spring.application.name:make-money}")
    private String applicationName;

    /**
     * 获取本节点所有线程池状态信息
     *
     * @return 线程池状态信息列表
     */
    public List<Map<String, Object>> getLocalThreadPoolInfo() {
        List<Map<String, Object>> list = new ArrayList<>();

        try {
            // 获取核心业务线程池信息
            ExecutorService coreExecutor = ThreadPoolUtil.getCoreExecutor();
            ThreadPoolExecutor coreThreadPoolExecutor = getThreadPoolExecutor(coreExecutor);
            if (coreThreadPoolExecutor != null) {
                Map<String, Object> coreInfo = new HashMap<>();
                coreInfo.put("name", "核心业务线程池");
                coreInfo.put("corePoolSize", coreThreadPoolExecutor.getCorePoolSize());
                coreInfo.put("maximumPoolSize", coreThreadPoolExecutor.getMaximumPoolSize());
                coreInfo.put("activeCount", coreThreadPoolExecutor.getActiveCount());
                coreInfo.put("poolSize", coreThreadPoolExecutor.getPoolSize());
                coreInfo.put("completedTaskCount", coreThreadPoolExecutor.getCompletedTaskCount());
                coreInfo.put("queueSize", coreThreadPoolExecutor.getQueue().size());
                coreInfo.put("queueRemainingCapacity", coreThreadPoolExecutor.getQueue().remainingCapacity());
                coreInfo.put("queueCapacity", coreThreadPoolExecutor.getQueue().size() + coreThreadPoolExecutor.getQueue().remainingCapacity());
                coreInfo.put("taskCount", coreThreadPoolExecutor.getTaskCount());
                list.add(coreInfo);

                logger.info("✅ 核心业务线程池监控信息: 核心线程数={}, 最大线程数={}, 活跃线程数={}, 当前线程数={}, 已完成任务数={}, 队列大小={}, 队列剩余容量={}, 总任务数={}",
                        coreThreadPoolExecutor.getCorePoolSize(),
                        coreThreadPoolExecutor.getMaximumPoolSize(),
                        coreThreadPoolExecutor.getActiveCount(),
                        coreThreadPoolExecutor.getPoolSize(),
                        coreThreadPoolExecutor.getCompletedTaskCount(),
                        coreThreadPoolExecutor.getQueue().size(),
                        coreThreadPoolExecutor.getQueue().remainingCapacity(),
                        coreThreadPoolExecutor.getTaskCount());
            } else {
                logger.warn("⚠️ 无法获取核心业务线程池ThreadPoolExecutor");
            }

            // 获取关注股票利润数据更新专用线程池信息
            ExecutorService watchStockExecutor = ThreadPoolUtil.getWatchStockExecutor();
            ThreadPoolExecutor watchStockThreadPoolExecutor = getThreadPoolExecutor(watchStockExecutor);
            if (watchStockThreadPoolExecutor != null) {
                Map<String, Object> watchStockInfo = new HashMap<>();
                watchStockInfo.put("name", "关注股票专用线程池");
                watchStockInfo.put("corePoolSize", watchStockThreadPoolExecutor.getCorePoolSize());
                watchStockInfo.put("maximumPoolSize", watchStockThreadPoolExecutor.getMaximumPoolSize());
                watchStockInfo.put("activeCount", watchStockThreadPoolExecutor.getActiveCount());
                watchStockInfo.put("poolSize", watchStockThreadPoolExecutor.getPoolSize());
                watchStockInfo.put("completedTaskCount", watchStockThreadPoolExecutor.getCompletedTaskCount());
                watchStockInfo.put("queueSize", watchStockThreadPoolExecutor.getQueue().size());
                watchStockInfo.put("queueRemainingCapacity", watchStockThreadPoolExecutor.getQueue().remainingCapacity());
                watchStockInfo.put("queueCapacity", watchStockThreadPoolExecutor.getQueue().size() + watchStockThreadPoolExecutor.getQueue().remainingCapacity());
                watchStockInfo.put("taskCount", watchStockThreadPoolExecutor.getTaskCount());
                list.add(watchStockInfo);

                logger.info("✅ 关注股票专用线程池监控信息: 核心线程数={}, 最大线程数={}, 活跃线程数={}, 当前线程数={}, 已完成任务数={}, 队列大小={}, 队列剩余容量={}, 总任务数={}",
                        watchStockThreadPoolExecutor.getCorePoolSize(),
                        watchStockThreadPoolExecutor.getMaximumPoolSize(),
                        watchStockThreadPoolExecutor.getActiveCount(),
                        watchStockThreadPoolExecutor.getPoolSize(),
                        watchStockThreadPoolExecutor.getCompletedTaskCount(),
                        watchStockThreadPoolExecutor.getQueue().size(),
                        watchStockThreadPoolExecutor.getQueue().remainingCapacity(),
                        watchStockThreadPoolExecutor.getTaskCount());
            } else {
                logger.warn("⚠️ 无法获取关注股票专用线程池ThreadPoolExecutor");
            }

            // 获取调度线程池信息
            ExecutorService scheduler = ThreadPoolUtil.getScheduler();
            if (scheduler instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor schedulerThreadPoolExecutor = (ThreadPoolExecutor) scheduler;
                Map<String, Object> schedulerInfo = new HashMap<>();
                schedulerInfo.put("name", "调度线程池");
                schedulerInfo.put("corePoolSize", schedulerThreadPoolExecutor.getCorePoolSize());
                schedulerInfo.put("maximumPoolSize", schedulerThreadPoolExecutor.getMaximumPoolSize());
                schedulerInfo.put("activeCount", schedulerThreadPoolExecutor.getActiveCount());
                schedulerInfo.put("poolSize", schedulerThreadPoolExecutor.getPoolSize());
                schedulerInfo.put("completedTaskCount", schedulerThreadPoolExecutor.getCompletedTaskCount());
                schedulerInfo.put("queueSize", schedulerThreadPoolExecutor.getQueue().size());
                schedulerInfo.put("queueRemainingCapacity", schedulerThreadPoolExecutor.getQueue().remainingCapacity());
                schedulerInfo.put("queueCapacity", schedulerThreadPoolExecutor.getQueue().size() + schedulerThreadPoolExecutor.getQueue().remainingCapacity());
                schedulerInfo.put("taskCount", schedulerThreadPoolExecutor.getTaskCount());
                list.add(schedulerInfo);

                logger.info("✅ 调度线程池监控信息: 核心线程数={}, 最大线程数={}, 活跃线程数={}, 当前线程数={}, 已完成任务数={}, 队列大小={}, 队列剩余容量={}, 总任务数={}",
                        schedulerThreadPoolExecutor.getCorePoolSize(),
                        schedulerThreadPoolExecutor.getMaximumPoolSize(),
                        schedulerThreadPoolExecutor.getActiveCount(),
                        schedulerThreadPoolExecutor.getPoolSize(),
                        schedulerThreadPoolExecutor.getCompletedTaskCount(),
                        schedulerThreadPoolExecutor.getQueue().size(),
                        schedulerThreadPoolExecutor.getQueue().remainingCapacity(),
                        schedulerThreadPoolExecutor.getTaskCount());
            } else {
                logger.warn("⚠️ 调度线程池不是ThreadPoolExecutor类型");
            }
        } catch (Exception e) {
            logger.error("💥 获取线程池信息失败", e);
        }

        return list;
    }

    /**
     * 获取线程池状态信息（用于本节点线程池信息展示）
     *
     * @return 线程池状态信息列表
     */
    public List<Map<String, Object>> getThreadPoolInfo() {
        return getLocalThreadPoolInfo();
    }

    /**
     * 获取集群环境下所有节点的线程池信息（从内存中获取）
     * 
     * @return 所有节点的线程池信息
     */
    public Map<String, Map<String, Object>> getClusterThreadPoolInfo() {
        if (clusterThreadPoolInfo == null) {
            return new HashMap<>();
        }
        // 先更新本节点信息
        updateLocalNodeInfo();
        
        // 返回所有节点信息
        return clusterThreadPoolInfo.getAllNodeThreadPoolInfo();
    }
    
    /**
     * 获取集群环境下所有节点的线程池信息（从Redis中获取）
     * 
     * @return 所有节点的线程池信息
     */
    public Map<String, Map<String, Object>> getClusterThreadPoolInfoFromRedis() {
        if (redisClusterThreadPoolService == null) {
            return new HashMap<>();
        }
        return redisClusterThreadPoolService.getAllNodeThreadPoolInfoFromRedis();
    }
    
    /**
     * 获取集群环境下所有节点的线程池聚合统计信息（从Redis中获取）
     * 
     * @return 聚合统计信息
     */
    public Map<String, Object> getAggregatedThreadPoolInfoFromRedis() {
        if (redisClusterThreadPoolService == null) {
            return new HashMap<>();
        }
        return redisClusterThreadPoolService.getAggregatedThreadPoolInfoFromRedis();
    }
    
    /**
     * 更新本节点线程池信息到集群信息中
     */
    public void updateLocalNodeInfo() {
        try {
            if (clusterThreadPoolInfo == null) {
                return;
            }
            // 获取本节点线程池信息
            List<Map<String, Object>> localInfoList = getLocalThreadPoolInfo();

            // 为了兼容旧的Map结构，这里暂时将List转换为Map
            Map<String, Object> localInfoMap = new HashMap<>();
            for (Map<String, Object> poolInfo : localInfoList) {
                String name = (String) poolInfo.get("name");
                if ("核心业务线程池".equals(name)) {
                    localInfoMap.put("coreExecutor", poolInfo);
                } else if ("关注股票专用线程池".equals(name)) {
                    localInfoMap.put("watchStockExecutor", poolInfo);
                } else if ("调度线程池".equals(name)) {
                    localInfoMap.put("scheduler", poolInfo);
                }
            }
            
            // 获取本节点标识（IP地址+应用名称）
            String localNodeId = IpUtils.getHostIp() + ":" + applicationName;
            
            // 更新到集群信息中
            clusterThreadPoolInfo.addNodeThreadPoolInfo(localNodeId, localInfoMap);
            
            logger.debug("🔄 更新本节点线程池信息到集群信息中: 节点={}, 信息={}", localNodeId, localInfoMap);
        } catch (Exception e) {
            logger.error("💥 更新本节点线程池信息失败", e);
        }
    }
    
    /**
     * 通过反射获取ThreadPoolExecutor实例
     * @param executorService ExecutorService实例
     * @return ThreadPoolExecutor实例，如果无法获取则返回null
     */
    private ThreadPoolExecutor getThreadPoolExecutor(ExecutorService executorService) {
        if (executorService instanceof ThreadPoolExecutor) {
            return (ThreadPoolExecutor) executorService;
        }
        
        try {
            // 尝试通过反射获取内部的ThreadPoolExecutor字段
            Field[] fields = executorService.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == ThreadPoolExecutor.class) {
                    field.setAccessible(true);
                    return (ThreadPoolExecutor) field.get(executorService);
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 通过反射获取ThreadPoolExecutor失败", e);
        }
        
        return null;
    }
}