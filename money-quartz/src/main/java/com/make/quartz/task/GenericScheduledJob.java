package com.make.quartz.task;

import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.executor.GenericTaskExecutor;
import com.make.quartz.util.QuartzJobWrapper;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通用 Quartz 任务（数据库配置任务的统一入口）
 */
@Component
public class GenericScheduledJob extends QuartzJobWrapper {

    private static final Logger log = LoggerFactory.getLogger(GenericScheduledJob.class);

    /**
     * 执行通用任务：委托给 GenericTaskExecutor
     *
     * @param context Quartz 上下文
     * @param sysJob  任务信息
     */
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        log.info("开始执行通用任务: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
        GenericTaskExecutor executor = SpringUtils.getBean(GenericTaskExecutor.class);
        executor.execute(sysJob);
        log.info("通用任务执行完成: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
    }
}
