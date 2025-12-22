//package com.make.quartz.util;
//
//import com.make.quartz.domain.SysJob;
//import org.quartz.CronScheduleBuilder;
//import org.quartz.CronTrigger;
//import org.quartz.Job;
//import org.quartz.JobBuilder;
//import org.quartz.JobDetail;
//import org.quartz.JobKey;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
//import org.quartz.TriggerBuilder;
//import org.quartz.TriggerKey;
//import com.make.common.constant.Constants;
//import com.make.common.constant.ScheduleConstants;
//import com.make.common.exception.job.TaskException;
//import com.make.common.exception.job.TaskException.Code;
//import com.make.common.utils.StringUtils;
//import com.make.common.utils.spring.SpringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * 定时任务工具类
// *
// * @author ruoyi
// */
//public class ScheduleUtils {
//    private static final Logger log = LoggerFactory.getLogger(ScheduleUtils.class);
//    /**
//     * 得到quartz任务类
//     *
//     * @param sysJob 执行计划
//     * @return 具体执行任务类
//     */
//    private static Class<? extends Job> getQuartzJobClass(SysJob sysJob) {
//        boolean isConcurrent = "0".equals(sysJob.getConcurrent());
//        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
//    }
//
//    /**
//     * 构建任务触发对象
//     */
//    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
//        return TriggerKey.triggerKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
//    }
//
//    /**
//     * 构建任务键对象
//     */
//    public static JobKey getJobKey(Long jobId, String jobGroup) {
//        return JobKey.jobKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
//    }
//
//    /**
//     * 创建定时任务
//     */
//    public static void createScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException, TaskException {
//        try {
//            log.debug("开始创建定时任务: ID={}, 名称={}, 组名={}", job.getJobId(), job.getJobName(), job.getJobGroup());
//
//            Class<? extends Job> jobClass = getQuartzJobClass(job);
//            // 构建job信息
//            Long jobId = job.getJobId();
//            String jobGroup = job.getJobGroup();
//            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();
//
//            // 表达式调度构建器
//            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
//            cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);
//
//            // 按新的cronExpression表达式构建一个新的trigger
//            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
//                    .withSchedule(cronScheduleBuilder).build();
//
//            // 放入参数，运行时的方法可以获取
//            jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job);
//
//            // 判断是否存在
//            JobKey jobKey = getJobKey(jobId, jobGroup);
//            if (scheduler.checkExists(jobKey)) {
//                log.warn("任务已存在，先删除再重新创建: ID={}, 名称={}, 组名={}", jobId, job.getJobName(), jobGroup);
//                // 防止创建时存在数据问题 先移除，然后在执行创建操作
//                scheduler.deleteJob(jobKey);
//            }
//
//            // 判断任务是否过期
//            if (StringUtils.isNotNull(CronUtils.getNextExecution(job.getCronExpression()))) {
//                log.debug("任务表达式有效，准备调度任务: ID={}, 名称={}", jobId, job.getJobName());
//                // 执行调度任务
//                scheduler.scheduleJob(jobDetail, trigger);
//            } else {
//                log.warn("任务表达式已过期，跳过调度: ID={}, 名称={}, 表达式={}", jobId, job.getJobName(), job.getCronExpression());
//            }
//
//            // 暂停任务
//            if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue())) {
//                scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
//                log.info("任务已暂停: ID={}, 名称={}", jobId, job.getJobName());
//            }
//
//            log.debug("定时任务创建完成: ID={}, 名称={}", jobId, job.getJobName());
//        } catch (SchedulerException | TaskException e) {
//            log.error("创建定时任务失败: ID={}, 名称={}, 错误信息: {}", job.getJobId(), job.getJobName(), e.getMessage(), e);
//            throw e;
//        } catch (Exception e) {
//            log.error("创建定时任务时发生未预期异常: ID={}, 名称={}, 错误信息: {}", job.getJobId(), job.getJobName(), e.getMessage(), e);
//            throw new TaskException("创建定时任务失败: " + e.getMessage(), Code.CONFIG_ERROR, e);
//        }
//    }
//
//    /**
//     * 删除定时任务
//     * 为防止数据不一致问题，增强删除操作的健壮性
//     */
//    public static void deleteScheduleJob(Scheduler scheduler, Long jobId, String jobGroup) throws SchedulerException {
//        try {
//            JobKey jobKey = getJobKey(jobId, jobGroup);
//            TriggerKey triggerKey = getTriggerKey(jobId, jobGroup);
//
//            // 先删除触发器
//            if (scheduler.checkExists(triggerKey)) {
//                scheduler.unscheduleJobs(java.util.Collections.singletonList(triggerKey));
//                log.debug("已删除任务触发器: ID={}, 组名={}", jobId, jobGroup);
//            }
//
//            // 再删除任务
//            if (scheduler.checkExists(jobKey)) {
//                scheduler.deleteJob(jobKey);
//                log.debug("已删除任务: ID={}, 组名={}", jobId, jobGroup);
//            }
//
//            log.info("✅ 定时任务删除成功: ID={}, 组名={}", jobId, jobGroup);
//        } catch (SchedulerException e) {
//            log.error("❌ 删除定时任务失败: ID={}, 组名={}, 错误信息: {}", jobId, jobGroup, e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    /**
//     * 设置定时任务策略
//     */
//    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJob job, CronScheduleBuilder cb)
//            throws TaskException {
//        switch (job.getMisfirePolicy()) {
//            case ScheduleConstants.MISFIRE_DEFAULT:
//                return cb;
//            case ScheduleConstants.MISFIRE_IGNORE_MISFIRES:
//                return cb.withMisfireHandlingInstructionIgnoreMisfires();
//            case ScheduleConstants.MISFIRE_FIRE_AND_PROCEED:
//                return cb.withMisfireHandlingInstructionFireAndProceed();
//            case ScheduleConstants.MISFIRE_DO_NOTHING:
//                return cb.withMisfireHandlingInstructionDoNothing();
//            default:
//                throw new TaskException("The task misfire policy '" + job.getMisfirePolicy()
//                        + "' cannot be used in cron schedule tasks", Code.CONFIG_ERROR);
//        }
//    }
//
//    /**
//     * 检查包名是否为白名单配置
//     *
//     * @param invokeTarget 目标字符串
//     * @return 结果
//     */
//    public static boolean whiteList(String invokeTarget) {
//        String packageName = StringUtils.substringBefore(invokeTarget, "(");
//        int count = StringUtils.countMatches(packageName, ".");
//        if (count > 1) {
//            return StringUtils.containsAnyIgnoreCase(invokeTarget, Constants.JOB_WHITELIST_STR);
//        }
//        Object obj = SpringUtils.getBean(StringUtils.split(invokeTarget, ".")[0]);
//        String beanPackageName = obj.getClass().getPackage().getName();
//        return StringUtils.containsAnyIgnoreCase(beanPackageName, Constants.JOB_WHITELIST_STR)
//                && !StringUtils.containsAnyIgnoreCase(beanPackageName, Constants.JOB_ERROR_STR);
//    }
//}
