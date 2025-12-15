package com.make.framework.web.domain.server;

import com.make.framework.config.ThreadPoolMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 线程池信息实体类
 *
 * @author make
 */
@Component
public class ThreadPoolInfo {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolInfo.class);

    private List<Map<String, Object>> threadPools;

    @Autowired
    private ThreadPoolMonitor threadPoolMonitor;

    public ThreadPoolInfo() {
        // 构造函数中不立即获取线程池信息，避免在Spring初始化过程中获取不到正确的值
    }

    /**
     * 初始化线程池信息
     */
    public void init() {
        try {
            this.threadPools = threadPoolMonitor.getThreadPoolInfo();
            logger.info("线程池信息初始化完成: 共获取到 {} 个线程池的信息。", threadPools.size());
            for (Map<String, Object> poolInfo : threadPools) {
                logger.info("线程池 '{}': {}", poolInfo.get("name"), poolInfo);
            }
        } catch (Exception e) {
            logger.error("初始化线程池信息失败", e);
            this.threadPools = new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getThreadPools() {
        return threadPools;
    }

    public void setThreadPools(List<Map<String, Object>> threadPools) {
        this.threadPools = threadPools;
    }
}