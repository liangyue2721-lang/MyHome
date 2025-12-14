package com.make.quartz.task;

import com.make.common.util.TraceIdUtil;
import com.make.quartz.mapper.SysJobLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时清理定时任务日志数据
 * 
 * 该任务会定期清理 sys_job_log 表中的历史数据，只保留最近3天的日志记录
 */
@Component("sysJobLogCleanupTask")
public class SysJobLogCleanupTask {
    
    private static final Logger log = LoggerFactory.getLogger(SysJobLogCleanupTask.class);
    
    @Autowired
    private SysJobLogMapper jobLogMapper;
    
    /**
     * 清理超过3天的定时任务日志数据
     * 
     * 该方法会删除 sys_job_log 表中创建时间早于3天前的所有记录
     */
    public void cleanUpJobLogs() {
        // 生成链路追踪ID并放入MDC
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        log.info("[{}] 开始执行定时任务日志清理任务", traceId);
        
        try {
            // 计算3天前的时间
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            String threeDaysAgoStr = threeDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            log.info("[{}] 清理 {} 之前的所有定时任务日志数据", traceId, threeDaysAgoStr);
            
            // 执行删除操作
            int deletedCount = jobLogMapper.deleteJobLogByLessThanDate(threeDaysAgoStr);
            
            log.info("[{}] 成功清理 {} 条定时任务日志数据", traceId, deletedCount);
        } catch (Exception e) {
            log.error("[{}] 执行定时任务日志清理任务时发生异常", traceId, e);
        } finally {
            // 清除链路追踪ID
            TraceIdUtil.clearTraceId();
        }
    }
}