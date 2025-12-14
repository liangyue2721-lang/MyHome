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
     * 用于跟踪正在执行的任务
     * key: jobKey, value: 执行开始时间
     */
    private static final ConcurrentHashMap<String, Long> executingJobs = new ConcurrentHashMap<>();

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
        this.taskDistributor = SpringUtils.getBean(TaskDistributor.class); // 初始化TaskDistributor
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
            if (RedisMessageQueue.isMessageProcessing(jobKey)) {
                log.warn("⏭️ 任务【{}】正在Redis消息队列中处理，跳过重复执行", jobKey);
                // 记录到监控系统，标记为跳过执行
                recordSkippedTask(sysJob, "任务正在Redis消息队列中处理");
                return;
            }

            // 尝试获取锁：等待 0 秒，锁定 60 秒，超过 60s 自动释放
            log.info("🔐 尝试获取任务分布式锁: {}", lockKey);
            locked = lock.tryLock(0, 60, TimeUnit.SECONDS);
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

            // 检查任务是否已分发但尚未完成
            if (taskDistributor.isTaskDistributed(jobKey)) {
                log.warn("⏭️ 任务【{}】已分发但尚未完成，跳过重复执行", jobKey);
                // 记录到监控系统，标记为跳过执行
                recordSkippedTask(sysJob, "任务已分发但尚未完成");
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
                // 记录到监控系统，标记为已分发
                recordDispatchedTask(sysJob);
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
        } finally {
            // 从执行中任务列表中移除
            executingJobs.remove(jobKey);
            log.info("🧹 任务【{}】已从执行中列表移除", jobKey);
            
            // 释放分布式锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 释放Quartz分布式锁: {}", lockKey);
            }
            // 释放任务锁
            taskDistributor.releaseTaskLock(jobKey);
            log.info("🧹 清理任务锁: {}", lockKey);
            
            // 记录任务执行完成
            taskMonitoringService.recordTaskComplete(jobKey);
            
            // 清除链路追踪ID
            TraceIdUtil.clearTraceId();
        }
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
            sysJobLog.setStatus(Constants.FAIL);
            sysJobLog.setJobMessage("任务跳过执行: " + reason);
            sysJobLog.setExceptionInfo("任务因" + reason + "被跳过执行");
            
            SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
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
            sysJobLog.setStatus(Constants.SUCCESS);
            sysJobLog.setJobMessage("任务已分发到其他节点执行");
            
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
            log.error("任务执行失败: {}", sysJob.getJobName(), e);
        } else {
            sysJobLog.setStatus(Constants.SUCCESS);
            log.info("任务执行成功: {}，耗时: {}ms", sysJob.getJobName(), runMs);
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
    
    /**
     * 检查任务是否正在执行
     * @param jobKey 任务键
     * @return true-正在执行，false-未在执行
     */
    public static boolean isJobExecuting(String jobKey) {
        return executingJobs.containsKey(jobKey);
    }
}