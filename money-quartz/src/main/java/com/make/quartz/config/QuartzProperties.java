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

    /**
     * 是否在系统启动后执行任务自恢复（扫描 sys_job 并将 nextTime 写入队列）
     */
    private boolean bootstrapEnabled = true;

    /**
     * 是否使用 Redis 分布式锁，确保多节点只有一个节点执行 bootstrap（推荐开启）
     */
    private boolean bootstrapUseRedisLock = true;

    /**
     * bootstrap 分布式锁 Key
     */
    private String bootstrapLockKey = "mq:bootstrap:lock";

    /**
     * bootstrap 分布式锁 TTL（秒）
     */
    private long bootstrapLockTtlSeconds = 60L;

    /**
     * bootstrap 重试次数（用于等待 Redis/DB 就绪）
     */
    private int bootstrapRetryTimes = 10;

    /**
     * bootstrap 重试间隔（毫秒）
     */
    private long bootstrapRetryIntervalMs = 3000L;

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

    public boolean isBootstrapEnabled() {
        return bootstrapEnabled;
    }

    public void setBootstrapEnabled(boolean bootstrapEnabled) {
        this.bootstrapEnabled = bootstrapEnabled;
    }

    public boolean isBootstrapUseRedisLock() {
        return bootstrapUseRedisLock;
    }

    public void setBootstrapUseRedisLock(boolean bootstrapUseRedisLock) {
        this.bootstrapUseRedisLock = bootstrapUseRedisLock;
    }

    public String getBootstrapLockKey() {
        return bootstrapLockKey;
    }

    public void setBootstrapLockKey(String bootstrapLockKey) {
        this.bootstrapLockKey = bootstrapLockKey;
    }

    public long getBootstrapLockTtlSeconds() {
        return bootstrapLockTtlSeconds;
    }

    public void setBootstrapLockTtlSeconds(long bootstrapLockTtlSeconds) {
        this.bootstrapLockTtlSeconds = bootstrapLockTtlSeconds;
    }

    public int getBootstrapRetryTimes() {
        return bootstrapRetryTimes;
    }

    public void setBootstrapRetryTimes(int bootstrapRetryTimes) {
        this.bootstrapRetryTimes = bootstrapRetryTimes;
    }

    public long getBootstrapRetryIntervalMs() {
        return bootstrapRetryIntervalMs;
    }

    public void setBootstrapRetryIntervalMs(long bootstrapRetryIntervalMs) {
        this.bootstrapRetryIntervalMs = bootstrapRetryIntervalMs;
    }
}
