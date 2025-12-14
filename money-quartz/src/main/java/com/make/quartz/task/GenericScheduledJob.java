package com.make.quartz.task;

import com.make.common.utils.bean.BeanUtils;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.executor.GenericTaskExecutor;
import com.make.quartz.util.JobInvokeUtil;
import com.make.quartz.util.ScheduleUtils;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通用定时任务类
 * 用于执行所有通过数据库配置的定时任务
 */
@Component
public class GenericScheduledJob extends AbstractScheduledTask {
    
    private static final Logger log = LoggerFactory.getLogger(GenericScheduledJob.class);
    
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        log.info("开始执行通用任务: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
        
        try {
            // 获取通用任务执行器并执行任务
            GenericTaskExecutor executor = SpringUtils.getBean(GenericTaskExecutor.class);
            executor.execute(sysJob);
            
            log.info("通用任务执行完成: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
        } catch (Exception e) {
            log.error("通用任务执行失败: {}[{}]", sysJob.getJobName(), sysJob.getJobId(), e);
            throw e;
        }
    }
}