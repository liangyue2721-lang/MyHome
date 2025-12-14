package com.make.quartz.util;

import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.service.ISysJobService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Quartz任务包装器
 * 用于将系统任务包装为Quartz可执行的任务，并支持分布式调度
 */
@DisallowConcurrentExecution
public class QuartzJobWrapper extends AbstractQuartzJob {
    
    private static final Logger log = LoggerFactory.getLogger(QuartzJobWrapper.class);
    
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        // 获取任务服务
        ISysJobService jobService = SpringUtils.getBean(ISysJobService.class);
        
        try {
            log.info("开始执行任务: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
            
            // 执行具体任务
            JobInvokeUtil.invokeMethod(sysJob);
            
            log.info("任务执行完成: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
        } catch (Exception e) {
            log.error("任务执行失败: {}[{}]", sysJob.getJobName(), sysJob.getJobId(), e);
            throw e;
        }
    }
}