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
     * Producer Thread Pool (Scheduler)
     */
    private int producerCoreSize = 4;
    private int producerMaxSize = 8;
    private int producerQueueCapacity = 1000;

    /**
     * Consumer Thread Pool (Stock)
     */
    private int consumerCoreSize = 8;
    private int consumerMaxSize = 16;
    private int consumerQueueCapacity = 2000;

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

    public int getProducerCoreSize() { return producerCoreSize; }
    public void setProducerCoreSize(int producerCoreSize) { this.producerCoreSize = producerCoreSize; }

    public int getProducerMaxSize() { return producerMaxSize; }
    public void setProducerMaxSize(int producerMaxSize) { this.producerMaxSize = producerMaxSize; }

    public int getProducerQueueCapacity() { return producerQueueCapacity; }
    public void setProducerQueueCapacity(int producerQueueCapacity) { this.producerQueueCapacity = producerQueueCapacity; }

    public int getConsumerCoreSize() { return consumerCoreSize; }
    public void setConsumerCoreSize(int consumerCoreSize) { this.consumerCoreSize = consumerCoreSize; }

    public int getConsumerMaxSize() { return consumerMaxSize; }
    public void setConsumerMaxSize(int consumerMaxSize) { this.consumerMaxSize = consumerMaxSize; }

    public int getConsumerQueueCapacity() { return consumerQueueCapacity; }
    public void setConsumerQueueCapacity(int consumerQueueCapacity) { this.consumerQueueCapacity = consumerQueueCapacity; }

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
