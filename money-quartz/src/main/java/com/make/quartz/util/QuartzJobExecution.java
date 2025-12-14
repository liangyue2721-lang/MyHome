package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理（允许并发执行）
 * 基于RetryableQuartzJob实现，支持任务失败重试机制
 *
 * @author ruoyi
 */
public class QuartzJobExecution extends RetryableQuartzJob {
    @Override
    protected void executeInternal(JobExecutionContext context, SysJob sysJob) throws Exception {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}