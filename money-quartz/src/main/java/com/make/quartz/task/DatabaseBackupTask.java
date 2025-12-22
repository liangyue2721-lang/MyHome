package com.make.quartz.task;

import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.executor.DatabaseBackupExecutor;
import com.make.quartz.util.QuartzJobWrapper;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据库备份 Quartz 任务
 */
@Component("databaseBackupTask")
public class DatabaseBackupTask extends QuartzJobWrapper {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupTask.class);

    /**
     * 执行数据库备份任务
     *
     * @param context Quartz 上下文
     * @param sysJob  任务信息
     */
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) {
        log.info("[DB-BACKUP] 开始执行数据库备份任务, jobId={}", sysJob.getJobId());
        DatabaseBackupExecutor executor = SpringUtils.getBean(DatabaseBackupExecutor.class);
        executor.executeBackup();
        log.info("[DB-BACKUP] 数据库备份任务执行完成, jobId={}", sysJob.getJobId());
    }
}
