package com.make.quartz.task;

import com.make.common.utils.ip.IpUtils;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.config.IpBlackListManager;
import com.make.quartz.config.RedisQuartzSemaphore;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobLog;
import com.make.quartz.repository.JobLogRepository;
import com.make.quartz.service.TaskMonitoringService;
import com.make.quartz.util.SchedulerManager;
import com.make.quartz.util.TaskDistributor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 抽象定时任务类
 * 实现统一的任务执行框架，包括分布式锁、日志记录、监控等功能
 */
public abstract class AbstractScheduledTask implements Job {
    
    private static final Logger log = LoggerFactory.getLogger(AbstractScheduledTask.class);
    
    /**
     * 用于跟踪正在执行的任务
     * key: jobKey, value: 执行开始时间
     */
    private static final ConcurrentHashMap<String, Long> executingJobs = new ConcurrentHashMap<>();
    
    /**
     * Redis 分布式锁工具，需要在 Spring 容器中注册
     */
    private RedisQuartzSemaphore redisQuartzSemaphore;
    
    /**
     * 调度管理器
     */
    private SchedulerManager schedulerManager;
    
    /**
     * 任务分发器
     */
    private TaskDistributor taskDistributor;
    
    /**
     * IP黑名单管理器
     */
    private IpBlackListManager ipBlackListManager;
    
    /**
     * 任务监控服务
     */
    private TaskMonitoringService taskMonitoringService;
    
    /**
     * 任务日志仓库
     */
    private JobLogRepository jobLogRepository;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 初始化依赖的服务
        initializeServices();
        
        SysJob sysJob = createSysJobFromContext(context);
        String jobKey = context.getJobDetail().getKey().toString();
        String lockKey = "quartz:lock:" + jobKey;
        RLock lock = redisQuartzSemaphore.getLock(lockKey);
        boolean locked = false;
        
        try {
            before(context, sysJob);
            
            log.info("🚀 开始执行任务: {}, 任务ID: {}", sysJob.getJobName(), jobKey);
            log.info("📋 任务详细信息 - ID: {}, 名称: {}, 组名: {}, 目标: {}, 状态: {}, 并发: {}, 主节点执行: {}", 
                    sysJob.getJobId(), sysJob.getJobName(), sysJob.getJobGroup(), 
                    sysJob.getInvokeTarget(), sysJob.getStatus(), sysJob.getConcurrent(), 
                    sysJob.getIsMasterNode());
            
            // 记录任务开始执行
            taskMonitoringService.recordTaskStart(jobKey);
            
            // 检查当前节点IP是否在黑名单中
            if (ipBlackListManager.isCurrentNodeIpBlacklisted()) {
                log.info("⏭️ 当前节点IP {} 在黑名单中，跳过任务【{}】执行", 
                        ipBlackListManager.getCurrentNodeIp(), jobKey);
                return;
            }
            
            // 检查是否需要主节点执行（通过Redis判断）
            String isMasterNode = "0"; // 默认值
            if (sysJob.getJobId() != null) {
                isMasterNode = schedulerManager.getJobIsMasterNode(sysJob.getJobId());
            }
            log.info("📋 任务 {} 的主节点执行要求: {}", jobKey, "1".equals(isMasterNode) ? "是" : "否");
            
            if ("1".equals(isMasterNode)) {
                // 需要主节点执行的任务
                if (!schedulerManager.isMasterNode()) {
                    log.info("⏭️ 任务【{}】需要主节点执行，当前节点不是主节点，跳过执行", jobKey);
                    return;
                }
                log.info("👑 任务【{}】由主节点执行，当前节点是主节点", jobKey);
            } else {
                log.info("📝 任务【{}】可在任意节点执行", jobKey);
            }
            
            // 检查任务是否正在Redis消息队列中处理
            if (com.make.quartz.util.RedisMessageQueue.isMessageProcessing(jobKey)) {
                log.warn("⏭️ 任务【{}】正在Redis消息队列中处理，跳过重复执行", jobKey);
                // 记录到监控系统，标记为跳过执行
                recordSkippedTask(sysJob, "任务正在Redis消息队列中处理");
                return;
            }
            
            // 尝试获取锁：开启看门狗（不设置leaseTime），等待0秒（立即返回）
            log.info("🔐 尝试获取任务分布式锁: {}", lockKey);
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("⏭️ 跳过任务【{}】，未获取到分布式锁，可能其他节点正在执行该任务", jobKey);
                // 记录到监控系统，标记为跳过执行
                recordSkippedTask(sysJob, "未能获取分布式锁");
                return;
            }
            log.info("✅ 成功获取任务分布式锁: {}", lockKey);
            
            // 检查任务是否已经在执行（本地检查）
            if (executingJobs.containsKey(jobKey)) {
                log.warn("⏭️ 任务【{}】已在执行中，跳过重复执行", jobKey);
                // 记录到监控系统，标记为跳过执行
                recordSkippedTask(sysJob, "任务已在执行中");
                return;
            }
            
            // 标记任务为正在执行
            executingJobs.put(jobKey, System.currentTimeMillis());
            log.info("🔖 任务【{}】标记为正在执行", jobKey);
            
            // 检查是否应该在当前节点执行任务（负载均衡）
            // 使用0.8作为负载阈值，当节点负载超过80%时考虑分发到其他节点
            log.info("⚖️ 检查任务 {} 是否应在当前节点执行", jobKey);
            if (!taskDistributor.shouldExecuteLocally(jobKey, 0.8)) {
                log.info("🔄 任务【{}】将分发到其他节点执行，当前节点跳过", jobKey);
                // 分发任务
                taskDistributor.distributeTask(sysJob);

                // 记录到监控系统，标记为已分发
                recordDispatchedTask(sysJob);

                // 分发后不再执行后续逻辑，finally块会处理锁释放
                return;
            }
            log.info("✅ 任务【{}】将在当前节点执行", jobKey);
            
            // 真正执行子类逻辑
            log.info("🔧 开始执行任务业务逻辑: {}", jobKey);
            doExecute(context, sysJob);
            log.info("✅ 任务业务逻辑执行完成: {}", jobKey);
            
            after(context, sysJob, null);
            log.info("🏁 任务【{}】执行完成", jobKey);
        } catch (Exception e) {
            log.error("❌ 任务执行异常 - {}", jobKey, e);
            after(context, sysJob, e);
            throw new JobExecutionException(e);
        } finally {
            // 从执行中任务列表中移除
            executingJobs.remove(jobKey);
            log.info("🧹 任务【{}】已从执行中列表移除", jobKey);
            
            // 释放分布式锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 释放Quartz分布式锁: {}", lockKey);
            }
            
            // 记录任务执行完成
            taskMonitoringService.recordTaskComplete(jobKey);
        }
    }
    
    /**
     * 初始化依赖的服务
     */
    private void initializeServices() {
        if (redisQuartzSemaphore == null) {
            redisQuartzSemaphore = SpringUtils.getBean(RedisQuartzSemaphore.class);
        }
        if (schedulerManager == null) {
            schedulerManager = SpringUtils.getBean(SchedulerManager.class);
        }
        if (taskDistributor == null) {
            taskDistributor = SpringUtils.getBean(TaskDistributor.class);
        }
        if (ipBlackListManager == null) {
            ipBlackListManager = SpringUtils.getBean(IpBlackListManager.class);
        }
        if (taskMonitoringService == null) {
            taskMonitoringService = SpringUtils.getBean(TaskMonitoringService.class);
        }
        if (jobLogRepository == null) {
            jobLogRepository = SpringUtils.getBean(JobLogRepository.class);
        }
    }
    
    /**
     * 从JobExecutionContext创建SysJob对象
     */
    private SysJob createSysJobFromContext(JobExecutionContext context) {
        SysJob sysJob = new SysJob();
        // 这里需要根据具体实现填充sysJob对象
        // 由于这是一个抽象类，具体的实现可能会有所不同
        return sysJob;
    }
    
    /**
     * 记录跳过的任务到监控系统
     * @param sysJob 任务信息
     * @param reason 跳过原因
     */
    private void recordSkippedTask(SysJob sysJob, String reason) {
        try {
            SysJobLog sysJobLog = new SysJobLog();
            sysJobLog.setJobName(sysJob.getJobName());
            sysJobLog.setJobGroup(sysJob.getJobGroup());
            sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
            sysJobLog.setStartTime(new Date());
            sysJobLog.setStopTime(new Date());
            sysJobLog.setHostIp(IpUtils.getHostIp());
            sysJobLog.setStatus(com.make.common.constant.Constants.FAIL);
            sysJobLog.setJobMessage("任务跳过执行: " + reason);
            sysJobLog.setExceptionInfo("任务因" + reason + "被跳过执行");
            
            jobLogRepository.recordFailure(sysJobLog, null);
        } catch (Exception e) {
            log.error("记录跳过的任务失败: {}", sysJob.getJobName(), e);
        }
    }
    
    /**
     * 记录已分发的任务到监控系统
     * @param sysJob 任务信息
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
            sysJobLog.setStatus(com.make.common.constant.Constants.SUCCESS);
            sysJobLog.setJobMessage("任务已分发到其他节点执行");
            
            jobLogRepository.recordSuccess(sysJobLog);
        } catch (Exception e) {
            log.error("记录分发的任务失败: {}", sysJob.getJobName(), e);
        }
    }
    
    /**
     * 执行前设置开始时间
     */
    protected void before(JobExecutionContext context, SysJob sysJob) {
        // 可以在此处添加前置处理逻辑
    }
    
    /**
     * 执行后记录日志
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e) {
        SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(new Date());
        sysJobLog.setStopTime(new Date());
        sysJobLog.setHostIp(IpUtils.getHostIp());
        
        long runMs = sysJobLog.getStopTime().getTime() - sysJobLog.getStartTime().getTime();
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        
        if (e != null) {
            sysJobLog.setStatus(com.make.common.constant.Constants.FAIL);
            String err = com.make.common.utils.StringUtils.substring(com.make.common.utils.ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(err);
            log.error("任务执行失败: {}", sysJob.getJobName(), e);
            jobLogRepository.recordFailure(sysJobLog, e);
        } else {
            sysJobLog.setStatus(com.make.common.constant.Constants.SUCCESS);
            log.info("任务执行成功: {}，耗时: {}ms", sysJob.getJobName(), runMs);
            jobLogRepository.recordSuccess(sysJobLog);
        }
    }
    
    /**
     * 抽象方法，由子类实现具体的任务执行逻辑
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}