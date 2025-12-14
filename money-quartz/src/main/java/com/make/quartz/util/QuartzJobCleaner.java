package com.make.quartz.util;

import com.make.common.constant.ScheduleConstants;
import com.make.quartz.domain.SysJob;
import com.make.quartz.mapper.SysJobMapper;
import com.make.quartz.service.ISysJobService;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Quartz任务清理工具类
 * 用于处理数据库中不一致的任务记录，确保Quartz调度器与数据库记录同步
 */
@Component
public class QuartzJobCleaner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QuartzJobCleaner.class);

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ISysJobService jobService;

    @Autowired
    private SysJobMapper jobMapper;

    /**
     * 应用启动后执行清理操作
     * @param args 启动参数
     * @throws Exception 异常
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 应用启动时执行Quartz任务清理...");
        try {
            cleanInconsistentJobs();
            log.info("✅ 应用启动时Quartz任务清理完成");
        } catch (Exception e) {
            log.error("❌ 应用启动时Quartz任务清理过程中发生异常", e);
        }
    }

    /**
     * 定时执行任务一致性检查，每30分钟执行一次
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void scheduledCleanInconsistentJobs() {
        log.info("⏰ 定时执行Quartz任务一致性检查...");
        try {
            cleanInconsistentJobs();
            log.info("✅ 定时Quartz任务一致性检查完成");
        } catch (Exception e) {
            log.error("❌ 定时Quartz任务一致性检查过程中发生异常", e);
        }
    }

    /**
     * 清理不一致的任务记录
     */
    public void cleanInconsistentJobs() {
        try {
            log.info("🔍 开始检查Quartz任务一致性...");

            // 获取数据库中的所有任务
            List<SysJob> dbJobs = jobMapper.selectJobAll();
            log.info("📋 数据库中任务数量: {}", dbJobs.size());

            // 获取调度器中的所有任务组
            List<String> jobGroups = scheduler.getJobGroupNames();
            log.info("📋 调度器中任务组数量: {}", jobGroups.size());

            // 收集调度器中的所有任务键
            List<JobKey> schedulerJobKeys = new ArrayList<>();
            for (String group : jobGroups) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
                schedulerJobKeys.addAll(jobKeys);
            }
            log.info("📋 调度器中任务数量: {}", schedulerJobKeys.size());

            // 检查调度器中存在但数据库中不存在的任务
            for (JobKey jobKey : schedulerJobKeys) {
                try {
                    // 检查数据库中是否存在对应的任务
                    boolean existsInDb = false;
                    String jobName = jobKey.getName();
                    String jobGroup = jobKey.getGroup();

                    // 从任务名称中提取任务ID（TASK_CLASS_NAME + ID格式）
                    if (jobName.startsWith(ScheduleConstants.TASK_CLASS_NAME)) {
                        String jobIdStr = jobName.substring(ScheduleConstants.TASK_CLASS_NAME.length());
                        try {
                            Long jobId = Long.parseLong(jobIdStr);
                            for (SysJob dbJob : dbJobs) {
                                if (dbJob.getJobId().equals(jobId) && dbJob.getJobGroup().equals(jobGroup)) {
                                    existsInDb = true;
                                    break;
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.warn("⚠️ 无法解析任务ID: {}", jobName);
                        }
                    }

                    if (!existsInDb) {
                        log.warn("🗑️ 发现不一致任务，调度器中有但数据库中没有: 名称={}, 组名={}", jobName, jobGroup);
                        try {
                            // 删除调度器中的不一致任务
                            scheduler.deleteJob(jobKey);
                            log.info("✅ 已删除不一致任务: 名称={}, 组名={}", jobName, jobGroup);
                        } catch (SchedulerException e) {
                            log.error("❌ 删除不一致任务失败: 名称={}, 组名={}, 错误: {}", jobName, jobGroup, e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ 检查任务时发生异常: JobKey={}", jobKey, e);
                }
            }

            // 检查数据库中存在但调度器中不存在的任务
            for (SysJob dbJob : dbJobs) {
                try {
                    JobKey jobKey = ScheduleUtils.getJobKey(dbJob.getJobId(), dbJob.getJobGroup());
                    if (!scheduler.checkExists(jobKey)) {
                        log.warn("🔄 发现缺失任务，数据库中有但调度器中没有: ID={}, 名称={}, 组名={}",
                                dbJob.getJobId(), dbJob.getJobName(), dbJob.getJobGroup());
                        try {
                            // 重新创建缺失的任务
                            ScheduleUtils.createScheduleJob(scheduler, dbJob);
                            log.info("✅ 已重新创建缺失任务: ID={}, 名称={}", dbJob.getJobId(), dbJob.getJobName());
                        } catch (Exception e) {
                            log.error("❌ 重新创建缺失任务失败: ID={}, 名称={}, 错误: {}",
                                    dbJob.getJobId(), dbJob.getJobName(), e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ 检查数据库任务时发生异常: ID={}, 名称={}", dbJob.getJobId(), dbJob.getJobName(), e);
                }
            }

            log.info("✅ Quartz任务一致性检查完成");
        } catch (Exception e) {
            log.error("❌ 执行Quartz任务一致性检查时发生异常", e);
        }
    }
}