package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * Quartz 任务执行器（禁止并发）
 *
 * <p>同一个 JobDetail 在上一次未结束时，不会并发触发下一次执行。
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends RetryableQuartzJob {

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
