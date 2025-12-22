package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.JobExecutionContext;

/**
 * Quartz Job 包装层（抽象）
 *
 * <p>职责：
 * <ul>
 *   <li>从 Quartz Context 中解析 SysJob</li>
 *   <li>把执行模板从 doExecute(context) 适配为 doExecute(context, sysJob)</li>
 * </ul>
 */
public abstract class QuartzJobWrapper extends AbstractQuartzJob {

    /**
     * 模板适配入口：解析 sysJob 后下沉给子类
     *
     * @param context Quartz 上下文
     */
    @Override
    protected final void doExecute(JobExecutionContext context) {
        SysJob sysJob = resolveSysJob(context);
        doExecute(context, sysJob);
    }

    /**
     * 子类业务实现入口（你当前所有 Quartz Job 都应该实现该方法）
     *
     * @param context Quartz 上下文
     * @param sysJob  SysJob 任务信息
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob);

    /**
     * 从 Quartz Context 中解析 SysJob
     *
     * <p>要求调度端把 SysJob 放入 mergedJobDataMap 的 "sysJob"。
     *
     * @param context Quartz 上下文
     * @return SysJob
     */
    protected SysJob resolveSysJob(JobExecutionContext context) {
        return (SysJob) context.getMergedJobDataMap().get("sysJob");
    }

    /**
     * 默认 jobKey：取 JobDetail Key Name（跨实例一致）
     *
     * @param context Quartz 上下文
     * @return jobKey
     */
    @Override
    protected String getJobKey(JobExecutionContext context) {
        return context.getJobDetail().getKey().getName();
    }
}
