package com.make.framework.config;

import com.make.framework.web.domain.server.ClusterThreadPoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 线程池信息定时更新任务
 * 定期将本节点的线程池信息更新到集群信息中
 *
 * @author make
 */
@Component
public class ThreadPoolInfoScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolInfoScheduler.class);
    
    @Autowired
    private ThreadPoolMonitor threadPoolMonitor;
    
    @Autowired
    private RedisClusterThreadPoolService redisClusterThreadPoolService;
    
    @Autowired
    private ServerInfoCollector serverInfoCollector;
    
    /**
     * 定时更新本节点线程池信息
     * 每10秒执行一次，以便更快看到效果
     */
    @Scheduled(fixedRate = 10000)
    public void updateThreadPoolInfo() {
        try {
            logger.info("🔄 开始更新本节点线程池信息");
            // 更新本地内存中的节点信息
            threadPoolMonitor.updateLocalNodeInfo();
            // 更新Redis中的节点信息
            redisClusterThreadPoolService.updateLocalNodeInfoToRedis();
            logger.info("✅ 本节点线程池信息更新完成");
        } catch (IllegalStateException e) {
            // Redis连接工厂被销毁
            logger.warn("定时更新线程池信息失败，Redis连接不可用");
        } catch (RedisConnectionFailureException e) {
            // Redis连接失败
            logger.warn("定时更新线程池信息失败，Redis连接失败: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("💥 定时更新线程池信息失败", e);
        }
    }
    
    /**
     * 定时收集本节点服务器信息
     * 每10秒执行一次，与线程池信息同步更新
     */
    @Scheduled(fixedRate = 10000)
    public void collectServerInfo() {
        try {
            logger.info("🔄 开始收集本节点服务器信息");
            // 收集并存储服务器信息到Redis
            serverInfoCollector.collectAndStoreServerInfo();
            logger.info("✅ 本节点服务器信息收集完成");
        } catch (IllegalStateException e) {
            // Redis连接工厂被销毁
            logger.warn("定时收集服务器信息失败，Redis连接不可用");
        } catch (RedisConnectionFailureException e) {
            // Redis连接失败
            logger.warn("定时收集服务器信息失败，Redis连接失败: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("💥 定时收集服务器信息失败", e);
        }
    }
}