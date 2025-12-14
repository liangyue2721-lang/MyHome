package com.make.quartz.repository.impl;

import com.make.common.utils.ExceptionUtil;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ip.IpUtils;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJobLog;
import com.make.quartz.repository.JobLogRepository;
import com.make.quartz.service.ISysJobLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * 任务日志仓库实现类
 * 负责任务日志的持久化操作
 */
@Repository
public class JobLogRepositoryImpl implements JobLogRepository {
    
    private static final Logger log = LoggerFactory.getLogger(JobLogRepositoryImpl.class);
    
    private ISysJobLogService jobLogService;
    
    public JobLogRepositoryImpl(ISysJobLogService jobLogService) {
        this.jobLogService = jobLogService;
    }
    
    @Override
    public void saveJobLog(SysJobLog jobLog) {
        try {
            jobLogService.addJobLog(jobLog);
        } catch (Exception e) {
            log.error("保存任务日志失败: {}", jobLog.getJobName(), e);
        }
    }
    
    @Override
    public void recordSuccess(SysJobLog jobLog) {
        jobLog.setStatus("0"); // 成功状态
        jobLog.setHostIp(IpUtils.getHostIp());
        saveJobLog(jobLog);
    }
    
    @Override
    public void recordFailure(SysJobLog jobLog, Exception exception) {
        jobLog.setStatus("1"); // 失败状态
        jobLog.setHostIp(IpUtils.getHostIp());
        if (exception != null) {
            String errMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(exception), 0, 2000);
            jobLog.setExceptionInfo(errMsg);
        }
        saveJobLog(jobLog);
    }
}