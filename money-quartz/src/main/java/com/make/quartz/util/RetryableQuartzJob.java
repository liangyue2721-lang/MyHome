package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 带重试机制的定时任务处理类
 * 
 * 该类继承自AbstractQuartzJob，增加了任务执行失败后的重试机制
 * 确保因临时问题未能执行的任务能够得到重试
 */
public abstract class RetryableQuartzJob extends AbstractQuartzJob {
    
    private static final Logger log = LoggerFactory.getLogger(RetryableQuartzJob.class);
    
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;
    
    /**
     * 重试间隔（毫秒）
     */
    private static final long RETRY_INTERVAL = 5000; // 5秒
    
    /**
     * 执行具体的定时任务逻辑，包含重试机制
     * 
     * @param context 任务执行上下文
     * @param sysJob  定时任务信息对象
     * @throws Exception 执行过程中可能抛出的异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                if (retryCount > 0) {
                    log.info("任务 {} 开始第 {} 次重试", sysJob.getJobName(), retryCount);
                    // 等待一段时间再重试
                    Thread.sleep(RETRY_INTERVAL);
                }
                
                // 执行实际的任务逻辑
                executeInternal(context, sysJob);
                // 执行成功，跳出循环
                return;
            } catch (Exception e) {
                retryCount++;
                lastException = e;
                log.warn("任务 {} 执行失败，已尝试 {} 次，错误信息: {}", 
                        sysJob.getJobName(), retryCount, e.getMessage(), e);
                
                // 如果达到最大重试次数，则抛出异常
                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("任务 {} 已达到最大重试次数 {}，任务执行最终失败", 
                            sysJob.getJobName(), MAX_RETRY_COUNT, e);
                    throw new Exception("任务执行失败，已重试 " + MAX_RETRY_COUNT + " 次", e);
                }
            }
        }
        
        // 如果循环结束仍未成功，则抛出最后一次异常
        if (lastException != null) {
            throw lastException;
        }
    }
    
    /**
     * 执行实际的任务逻辑，由子类实现
     * 
     * @param context 任务执行上下文
     * @param sysJob  定时任务信息对象
     * @throws Exception 执行过程中可能抛出的异常
     */
    protected abstract void executeInternal(JobExecutionContext context, SysJob sysJob) throws Exception;
}