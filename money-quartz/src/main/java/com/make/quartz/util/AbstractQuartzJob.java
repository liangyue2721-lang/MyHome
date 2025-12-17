package com.make.quartz.util;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.make.common.util.TraceIdUtil;
import com.make.common.utils.ip.IpUtils;
import com.make.quartz.config.IpBlackListManager;
import com.make.quartz.config.RedisQuartzSemaphore;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobLog;
import com.make.quartz.service.ISysJobLogService;
import com.make.quartz.service.TaskMonitoringService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.make.common.constant.Constants;
import com.make.common.constant.ScheduleConstants;
import com.make.common.utils.ExceptionUtil;
import com.make.common.utils.StringUtils;
import com.make.common.utils.bean.BeanUtils;
import com.make.common.utils.spring.SpringUtils;

/**
 * 带分布式锁的抽象 Quartz Job
 */
public abstract class AbstractQuartzJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    /**
     * Redis 分布式锁工具，需要在 Spring 容器中注册
     */
    private final RedisQuartzSemaphore redisQuartzSemaphore;
    
    /**
     * 调度管理器
     */
    private final SchedulerManager schedulerManager;
    
    /**
     * 任务分发器
     */
    private final TaskDistributor taskDistributor;
    
    /**
     * IP黑名单管理器
     */
    private final IpBlackListManager ipBlackListManager;
    
    /**
     * 任务监控服务
     */
    private final TaskMonitoringService taskMonitoringService;
    
    public AbstractQuartzJob() {
        // 通过 SpringUtils 获取已注册的 Bean
        this.redisQuartzSemaphore = SpringUtils.getBean(RedisQuartzSemaphore.class);
        this.schedulerManager = SpringUtils.getBean(SchedulerManager.class);
        this.taskDistributor = SpringUtils.getBean(TaskDistributor.class);
        this.ipBlackListManager = SpringUtils.getBean(IpBlackListManager.class);
        this.taskMonitoringService = SpringUtils.getBean(TaskMonitoringService.class);
    }

    @Override
    public void execute(JobExecutionContext context) {
        // 生成链路追踪ID并放入MDC
        String traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.putTraceId(traceId);
        
        SysJob sysJob = new SysJob();
        BeanUtils.copyBeanProp(sysJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));

        String jobKey = sysJob.getJobGroup() + "." + sysJob.getJobName();
        String lockKey = "quartz:lock:" + jobKey;
        RLock lock = redisQuartzSemaphore.getLock(lockKey);
        boolean locked = false;

        try {
            before(context, sysJob);
            
            // 检查当前节点IP是否在黑名单中
            if (ipBlackListManager.isCurrentNodeIpBlacklisted()) {
                log.info("⏭️ 当前节点IP {} 在黑名单中，跳过任务【{}】执行", 
                        ipBlackListManager.getCurrentNodeIp(), jobKey);
                return;
            }

            // 检查是否需要主节点执行
            String isMasterNode = "0";
            if (sysJob.getJobId() != null) {
                isMasterNode = schedulerManager.getJobIsMasterNode(sysJob.getJobId());
            }
            
            if ("1".equals(isMasterNode)) {
                if (!schedulerManager.isMasterNode()) {
                    log.info("⏭️ 任务【{}】需要主节点执行，当前节点不是主节点", jobKey);
                    return;
                }
            }

            // 尝试获取锁：开启看门狗（不设置leaseTime），等待0秒（立即返回）
            // 如果已经被锁，说明其他节点或线程正在运行
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                // 记录日志或指标：跳过执行
                return;
            }

            // 获取锁成功，检查是否应该在本地执行
            if (!taskDistributor.shouldExecuteLocally(jobKey, 0.8)) {
                // 负载过高，分发任务
                log.info("🔄 任务【{}】负载过高，分发到全局队列", jobKey);
                taskDistributor.distributeTask(sysJob);

                // 必须释放锁，以便消费者能获取锁并执行
                lock.unlock();
                locked = false;

                recordDispatchedTask(sysJob);
                return;
            }

            // 真正执行子类逻辑
            taskMonitoringService.recordTaskStart(jobKey);
            doExecute(context, sysJob);

            after(context, sysJob, null);
        } catch (Exception e) {
            log.error("❌ 任务执行异常 - {}", jobKey, e);
            after(context, sysJob, e);
        } finally {
            // 释放分布式锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            
            if (locked) { // 只有真正执行完成才记录结束（分发的任务由消费者记录）
                taskMonitoringService.recordTaskComplete(jobKey);
            }
            
            // 清除链路追踪ID
            TraceIdUtil.clearTraceId();
        }
    }
    
    /**
     * 记录已分发的任务到监控系统
     */
    private void recordDispatchedTask(SysJob sysJob) {
        try {
            SysJobLog sysJobLog = new SysJobLog();
            sysJobLog.setJobName(sysJob.getJobName());
            sysJobLog.setJobGroup(sysJob.getJobGroup());
            sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
            sysJobLog.setStartTime(new Date());
            sysJobLog.setStopTime(new Date());
            sysJobLog.setHostIp(IpUtils.getHostIp());
            sysJobLog.setStatus(Constants.SUCCESS);
            sysJobLog.setJobMessage("任务已分发到全局队列");
            
            SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
        } catch (Exception e) {
            log.error("记录分发的任务失败: {}", sysJob.getJobName(), e);
        }
    }

    /**
     * 执行前设置开始时间
     */
    protected void before(JobExecutionContext context, SysJob sysJob) {
        threadLocal.set(new Date());
    }

    /**
     * 执行后记录日志
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e) {
        Date startTime = threadLocal.get();
        threadLocal.remove();

        // 避免NPE：如果startTime为空，默认当前时间
        if (startTime == null) {
            startTime = new Date();
        }

        SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setStopTime(new Date());
        sysJobLog.setHostIp(IpUtils.getHostIp());

        long runMs = sysJobLog.getStopTime().getTime() - sysJobLog.getStartTime().getTime();
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");

        if (e != null) {
            sysJobLog.setStatus(Constants.FAIL);
            String err = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(err);
        } else {
            sysJobLog.setStatus(Constants.SUCCESS);
        }

        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    /**
     * 线程池执行器
     *
     * @param context  工作执行上下文对象
     * @param sysJob 系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
