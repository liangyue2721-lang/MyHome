package com.make.quartz.task;

import com.make.common.constant.KafkaTopics;
import com.make.quartz.domain.SysJob;
import com.make.quartz.util.QuartzJobWrapper;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 数据库备份 Quartz 任务
 */
@Component("databaseBackupTask")
public class DatabaseBackupTask extends QuartzJobWrapper {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 执行数据库备份任务
     *
     * @param context Quartz 上下文
     * @param sysJob  任务信息
     */
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) {
        String traceId = UUID.randomUUID().toString();
        log.info("[DB-BACKUP] 触发数据库备份任务, jobId={}, topic={}, traceId={}", sysJob.getJobId(), KafkaTopics.TOPIC_SYSTEM_BACKUP, traceId);
        kafkaTemplate.send(KafkaTopics.TOPIC_SYSTEM_BACKUP, traceId, "trigger");
        log.info("[DB-BACKUP] 数据库备份任务触发消息已发送, jobId={}", sysJob.getJobId());
    }
}
