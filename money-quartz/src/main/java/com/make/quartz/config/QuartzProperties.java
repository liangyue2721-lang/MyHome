package com.make.quartz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Quartz 任务调度配置属性
 */
@Component
@ConfigurationProperties(prefix = "quartz")
public class QuartzProperties {

    /**
     * 监听器核心线程倍数 (相对于CPU核数)
     */
    private int listenerCoreThreadsMultiple = 1;

    /**
     * 监听器最大线程倍数 (相对于CPU核数)
     */
    private int listenerMaxThreadsMultiple = 2;

    /**
     * 任务执行器核心线程倍数 (相对于CPU核数)
     */
    private int executorCoreThreadsMultiple = 4;

    /**
     * 任务执行器最大线程倍数 (相对于CPU核数)
     */
    private int executorMaxThreadsMultiple = 8;

    /**
     * 任务最大重试次数
     */
    private int maxRetryCount = 3;

    /**
     * 任务执行超时时间 (毫秒)，默认5分钟
     */
    private long taskTimeout = 300000;

    /**
     * 任务分发器线程数
     */
    private int distributorThreads = 2;

    public int getListenerCoreThreadsMultiple() {
        return listenerCoreThreadsMultiple;
    }

    public void setListenerCoreThreadsMultiple(int listenerCoreThreadsMultiple) {
        this.listenerCoreThreadsMultiple = listenerCoreThreadsMultiple;
    }

    public int getListenerMaxThreadsMultiple() {
        return listenerMaxThreadsMultiple;
    }

    public void setListenerMaxThreadsMultiple(int listenerMaxThreadsMultiple) {
        this.listenerMaxThreadsMultiple = listenerMaxThreadsMultiple;
    }

    public int getExecutorCoreThreadsMultiple() {
        return executorCoreThreadsMultiple;
    }

    public void setExecutorCoreThreadsMultiple(int executorCoreThreadsMultiple) {
        this.executorCoreThreadsMultiple = executorCoreThreadsMultiple;
    }

    public int getExecutorMaxThreadsMultiple() {
        return executorMaxThreadsMultiple;
    }

    public void setExecutorMaxThreadsMultiple(int executorMaxThreadsMultiple) {
        this.executorMaxThreadsMultiple = executorMaxThreadsMultiple;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getTaskTimeout() {
        return taskTimeout;
    }

    public void setTaskTimeout(long taskTimeout) {
        this.taskTimeout = taskTimeout;
    }

    public int getDistributorThreads() {
        return distributorThreads;
    }

    public void setDistributorThreads(int distributorThreads) {
        this.distributorThreads = distributorThreads;
    }
}
