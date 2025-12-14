package com.make.quartz.repository;

import com.make.quartz.domain.SysJobLog;
import org.springframework.stereotype.Repository;

/**
 * 任务日志仓库接口
 * 负责任务日志的持久化操作
 */
public interface JobLogRepository {
    
    /**
     * 保存任务日志
     * @param jobLog 任务日志信息
     */
    void saveJobLog(SysJobLog jobLog);
    
    /**
     * 记录任务执行成功
     * @param jobLog 任务日志信息
     */
    void recordSuccess(SysJobLog jobLog);
    
    /**
     * 记录任务执行失败
     * @param jobLog 任务日志信息
     * @param exception 异常信息
     */
    void recordFailure(SysJobLog jobLog, Exception exception);
}