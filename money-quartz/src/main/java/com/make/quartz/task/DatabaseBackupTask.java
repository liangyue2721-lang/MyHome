package com.make.quartz.task;

import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.executor.DatabaseBackupExecutor;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据库备份定时任务组件
 *
 * <p>功能：
 * - 仅在 Linux 系统上执行 mysqldump 完整备份（表结构 + 数据 + 存储过程 + 触发器 + 事件）。
 * - 备份文件保存路径：/home/Sql/yyyy-MM-dd/{database}_backup_yyyyMMddHHmmss.sql
 * - 使用 --result-file= 方式输出文件，避免 ProcessBuilder 无法处理 shell 重定向导致文件为空。
 * </p>
 */
@Component("databaseBackupTask")
public class DatabaseBackupTask extends AbstractScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupTask.class);

    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        log.info("[DB-BACKUP] 开始执行数据库备份任务");
        
        // 获取任务执行器并执行备份任务
        DatabaseBackupExecutor executor = SpringUtils.getBean(DatabaseBackupExecutor.class);
        executor.executeBackup();
        
        log.info("[DB-BACKUP] 数据库备份任务执行完成");
    }
}