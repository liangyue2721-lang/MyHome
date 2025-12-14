package com.make.quartz.executor;

import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.service.ISysJobLogService;
import com.make.quartz.util.JobInvokeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通用任务执行器
 * 负责执行各种类型的任务
 */
@Component
public class GenericTaskExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(GenericTaskExecutor.class);
    
    /**
     * 执行任务
     * @param sysJob 任务信息
     */
    public void execute(SysJob sysJob) throws Exception {
        log.info("开始执行任务: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
        
        try {
            // 执行具体任务
            JobInvokeUtil.invokeMethod(sysJob);
            
            log.info("任务执行完成: {}[{}]", sysJob.getJobName(), sysJob.getJobId());
        } catch (Exception e) {
            log.error("任务执行失败: {}[{}]", sysJob.getJobName(), sysJob.getJobId(), e);
            throw e;
        }
    }
}