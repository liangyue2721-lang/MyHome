package com.make.framework.web.domain.server;

import com.make.framework.config.ThreadPoolMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static com.make.common.core.text.Convert.toInt;
import static com.make.common.core.text.Convert.toLong;

/**
 * 线程池信息实体类
 *
 * @author make
 */
@Component
public class ThreadPoolInfo {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolInfo.class);

    /**
     * 核心线程数
     */
    private int corePoolSize;

    /**
     * 最大线程数
     */
    private int maximumPoolSize;

    /**
     * 当前活跃线程数
     */
    private int activeCount;

    /**
     * 当前线程池大小
     */
    private int poolSize;

    /**
     * 已完成任务数
     */
    private long completedTaskCount;

    /**
     * 队列中等待执行的任务数
     */
    private int queueSize;

    /**
     * 队列剩余容量
     */
    private int queueRemainingCapacity;

    /**
     * 总任务数
     */
    private long taskCount;

    /**
     * 核心业务线程池信息
     */
    private Map<String, Object> coreExecutor;

    /**
     * 关注股票专用线程池信息
     */
    private Map<String, Object> watchStockExecutor;

    /**
     * 调度线程池信息
     */
    private Map<String, Object> scheduler;

    @Autowired
    private ThreadPoolMonitor threadPoolMonitor;

    public ThreadPoolInfo() {
        // 构造函数中不立即获取线程池信息，避免在Spring初始化过程中获取不到正确的值
    }

    /**
     * 初始化线程池信息
     */
    public void init() {
        // 从ThreadPoolMonitor获取线程池信息
        try {
            Map<String, Object> info = threadPoolMonitor.getThreadPoolInfo();
            // 获取核心业务线程池信息
            Object coreExecObj = info.get("coreExecutor");
            this.coreExecutor = coreExecObj instanceof Map
                    ? (Map<String, Object>) coreExecObj
                    : Collections.emptyMap();

            if (!coreExecutor.isEmpty()) {
                this.corePoolSize = toInt(coreExecutor.get("corePoolSize"));
                this.maximumPoolSize = toInt(coreExecutor.get("maximumPoolSize"));
                this.activeCount = toInt(coreExecutor.get("activeCount"));
                this.poolSize = toInt(coreExecutor.get("poolSize"));

                this.completedTaskCount = toLong(coreExecutor.get("completedTaskCount"));
                this.queueSize = toInt(coreExecutor.get("queueSize"));
                this.queueRemainingCapacity = toInt(coreExecutor.get("queueRemainingCapacity"));
                this.taskCount = toLong(coreExecutor.get("taskCount"));
            }

           // 获取关注股票专用线程池信息
            Object watchObj = info.get("watchStockExecutor");
            this.watchStockExecutor = watchObj instanceof Map
                    ? (Map<String, Object>) watchObj
                    : Collections.emptyMap();

          // 获取调度线程池信息
            Object schedulerObj = info.get("scheduler");
            this.scheduler = schedulerObj instanceof Map
                    ? (Map<String, Object>) schedulerObj
                    : Collections.emptyMap();


            logger.info("线程池信息初始化完成: 核心线程数={}, 最大线程数={}, 活跃线程数={}, 当前线程数={}, 已完成任务数={}, 队列大小={}, 队列剩余容量={}, 总任务数={}",
                    this.corePoolSize,
                    this.maximumPoolSize,
                    this.activeCount,
                    this.poolSize,
                    this.completedTaskCount,
                    this.queueSize,
                    this.queueRemainingCapacity,
                    this.taskCount);

            logger.info("所有线程池信息: 核心业务线程池={}, 关注股票专用线程池={}, 调度线程池={}",
                    this.coreExecutor,
                    this.watchStockExecutor,
                    this.scheduler);
        } catch (Exception e) {
            logger.error("初始化线程池信息失败", e);
        }
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getQueueRemainingCapacity() {
        return queueRemainingCapacity;
    }

    public void setQueueRemainingCapacity(int queueRemainingCapacity) {
        this.queueRemainingCapacity = queueRemainingCapacity;
    }

    public long getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }

    public Map<String, Object> getCoreExecutor() {
        return coreExecutor;
    }

    public void setCoreExecutor(Map<String, Object> coreExecutor) {
        this.coreExecutor = coreExecutor;
    }

    public Map<String, Object> getWatchStockExecutor() {
        return watchStockExecutor;
    }

    public void setWatchStockExecutor(Map<String, Object> watchStockExecutor) {
        this.watchStockExecutor = watchStockExecutor;
    }

    public Map<String, Object> getScheduler() {
        return scheduler;
    }

    public void setScheduler(Map<String, Object> scheduler) {
        this.scheduler = scheduler;
    }
}