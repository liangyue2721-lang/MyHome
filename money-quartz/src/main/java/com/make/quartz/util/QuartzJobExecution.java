package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.JobExecutionContext;

/**
 * Quartz 任务执行器（允许并发）
 *
 * <p>用途：执行数据库配置的 invokeTarget 对应的方法。
 */
public class QuartzJobExecution extends RetryableQuartzJob {

    /**
     * 一次执行逻辑：直接调用 JobInvokeUtil
     *
     * @param context Quartz 上下文
     * @param sysJob  任务信息
     */
    @Override
    protected void executeInternal(JobExecutionContext context, SysJob sysJob) throws Exception {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
