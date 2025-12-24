package com.make.framework.config;

import com.make.framework.web.domain.server.ClusterThreadPoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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

    private volatile boolean running = true;
    private Thread schedulerThread;
    
    @PostConstruct
    public void init() {
        schedulerThread = new Thread(() -> {
            logger.info("启动线程池信息更新线程");
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    updateThreadPoolInfo();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.info("线程池信息更新线程被中断");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("线程池信息更新线程异常", e);
                    // 避免死循环狂刷日志
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            logger.info("线程池信息更新线程已停止");
        }, "ThreadPoolInfoScheduler-Thread");

        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
    }
    
    /**
     * 定时更新本节点线程池信息
     * 每10秒执行一次，以便更快看到效果
     */
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
}