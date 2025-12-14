package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理类（禁止并发执行）
 * <p>
 * 该类继承自RetryableQuartzJob，通过添加@DisallowConcurrentExecution注解，
 * 确保同一任务实例不会并发执行，即当前一个任务实例正在执行时，
 * 不会同时启动另一个相同的任务实例
 * </p>
 *
 * @author ruoyi
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends RetryableQuartzJob {
    /**
     * 执行具体的定时任务逻辑
     * <p>
     * 该方法通过JobInvokeUtil工具类调用SysJob对象中指定的业务方法
     * </p>
     *
     * @param context 任务执行上下文
     * @param sysJob  定时任务信息对象
     * @throws Exception 执行过程中可能抛出的异常
     */
    @Override
    protected void executeInternal(JobExecutionContext context, SysJob sysJob) throws Exception {
        // 调用JobInvokeUtil的invokeMethod方法执行定时任务
        JobInvokeUtil.invokeMethod(sysJob);
    }
}