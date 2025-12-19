package com.make.quartz.service;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.util.JobInvokeUtil;
import com.make.quartz.util.RedisMessageQueue;
import com.make.quartz.util.SchedulerManager;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行服务 (Task Execution Service)
 *
 * 职责：
 * 1. 监听 Redis 队列，接收分发到本节点的任务
 * 2. 统一执行入口 `executeSysJob`
 * 3. 管理执行线程池，防止过载
 *
 * 日志前缀说明：
 * - [EXEC_MSG_RCV]: 收到任务消息
 * - [EXEC_SUBMIT]: 提交到本地线程池
 * - [JOB-EXEC-START]: 开始执行业务逻辑
 * - [JOB-EXEC-END]: 业务逻辑执行完成
 * - [JOB-EXEC-ERROR]: 业务逻辑执行异常
 */
@Service
public class TaskExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);
    
    @Autowired
    private Scheduler scheduler;
    
    private ThreadPoolExecutor taskExecutor;
    
    private static final ConcurrentHashMap<String, Long> executingTasks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        this.taskExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                Runtime.getRuntime().availableProcessors() * 8,
                10000,
                "TaskExecutionThread"
        );
        
        String currentNodeId = SchedulerManager.getCurrentNodeId();
        log.info("初始化任务执行服务 | NodeID: {}", currentNodeId);
        
        RedisMessageQueue.getInstance().startListening(currentNodeId, this::handleTaskMessage);
    }
    
    @PreDestroy
    public void destroy() {
        log.info("停止任务执行服务");
        RedisMessageQueue.getInstance().stopListening();
        
        if (taskExecutor != null && !taskExecutor.isShutdown()) {
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    taskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void handleTaskMessage(RedisMessageQueue.TaskMessage message) {
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("[EXEC_MSG_RCV] 收到任务 | TaskID: {} | Node: {}", message.getTaskId(), message.getTargetNode());
            }
            
            if (executingTasks.containsKey(message.getTaskId())) {
                log.warn("[EXEC_SKIP] 任务 {} 已在执行中", message.getTaskId());
                return;
            }

            // 同步执行任务，确保任务执行完成后才返回，从而触发外部的ACK
            try {
                executingTasks.put(message.getTaskId(), System.currentTimeMillis());

                SysJob sysJob = reconstructSysJob(message);
                if (sysJob != null) {
                    executeSysJob(sysJob);
                } else {
                    executeTask(message.getTaskId());
                }

            } catch (Exception e) {
                log.error("[EXEC_FAIL] 任务执行异常 | TaskID: {}", message.getTaskId(), e);
                // 抛出异常以触发Requeue
                throw new RuntimeException(e);
            } finally {
                executingTasks.remove(message.getTaskId());
            }
            
        } catch (Exception e) {
            log.error("[EXEC_ERROR] 执行任务失败 | TaskID: {}", message.getTaskId(), e);
            throw e;
        }
    }
    
    private SysJob reconstructSysJob(RedisMessageQueue.TaskMessage message) {
        if (message.getPayload() != null && !message.getPayload().isEmpty()) {
            try {
                SysJob sysJob = JSON.parseObject(JSON.toJSONString(message.getPayload()), SysJob.class);
                if (sysJob != null) {
                    sysJob.setTraceId(message.getTraceId());
                    return sysJob;
                }
            } catch (Exception e) {
                log.warn("[EXEC_RECONSTRUCT_FAIL] 从Payload恢复SysJob失败", e);
            }
        }
        return null;
    }

    /**
     * 统一执行入口
     */
    public void executeSysJob(SysJob sysJob) throws Exception {
        long startTime = System.currentTimeMillis();
        String traceId = sysJob.getTraceId();
        String jobName = sysJob.getJobName();

        log.info("[JOB-EXEC-START] 开始执行任务 | Job: {} | TraceId: {}", jobName, traceId);

        try {
            JobInvokeUtil.invokeMethod(sysJob);
            
            long endTime = System.currentTimeMillis();
            log.info("[JOB-EXEC-END] 任务执行成功 | Job: {} | TraceId: {} | Cost: {}ms", jobName, traceId, (endTime - startTime));
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("[JOB-EXEC-ERROR] 任务执行失败 | Job: {} | TraceId: {} | Cost: {}ms", jobName, traceId, (endTime - startTime), e);
            throw e;
        }
    }

    private void executeTask(String taskId) throws Exception {
        log.info("[EXEC_LEGACY] 使用旧方式执行任务: {}", taskId);

        if (taskId == null || !taskId.contains(".")) {
            log.error("任务ID格式错误: {}", taskId);
            return;
        }

        String[] parts = taskId.split("\\.", 2);
        String jobGroup = parts[0];
        String jobName = parts[1];

        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        if (jobDetail == null) {
            log.error("未找到任务详情: {}", taskId);
            return;
        }

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        SysJob sysJob = new SysJob();
        sysJob.setJobName(jobName);
        sysJob.setJobGroup(jobGroup);

        Object jobProperties = jobDataMap.get("TASK_PROPERTIES");
        if (jobProperties != null) {
            copyBeanProp(sysJob, jobProperties);
        } else {
            if (jobDataMap.containsKey("invokeTarget")) {
                sysJob.setInvokeTarget(jobDataMap.getString("invokeTarget"));
            }
        }

        if (sysJob.getInvokeTarget() != null) {
            executeSysJob(sysJob);
        } else {
             log.error("无法执行任务，invokeTarget为空: {}", taskId);
        }
    }
    
    private void copyBeanProp(Object dest, Object src) {
        try {
            String json = JSON.toJSONString(src);
            SysJob sourceJob = JSON.parseObject(json, SysJob.class);

            if (dest instanceof SysJob) {
                SysJob destJob = (SysJob) dest;
                destJob.setInvokeTarget(sourceJob.getInvokeTarget());
                destJob.setCronExpression(sourceJob.getCronExpression());
                destJob.setMisfirePolicy(sourceJob.getMisfirePolicy());
                destJob.setConcurrent(sourceJob.getConcurrent());
                destJob.setStatus(sourceJob.getStatus());
            }
        } catch (Exception e) {
            log.error("复制Bean属性异常", e);
        }
    }
    
    public static boolean isTaskExecuting(String taskId) {
        return executingTasks.containsKey(taskId);
    }
}
