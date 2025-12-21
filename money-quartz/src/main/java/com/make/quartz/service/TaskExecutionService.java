package com.make.quartz.service;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
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
    
    // Key: Execution ID (Instance), Value: StartTime
    // Tracks currently executing instances on this node
    private static final ConcurrentHashMap<String, Long> executingTasks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        String currentNodeId = SchedulerManager.getCurrentNodeId();
        log.info("初始化任务执行服务 | NodeID: {}", currentNodeId);
        
        RedisMessageQueue.getInstance().startListening(currentNodeId, this::handleTaskMessage);
    }
    
    @PreDestroy
    public void destroy() {
        log.info("停止任务执行服务");
        RedisMessageQueue.getInstance().stopListening();
    }
    
    private void handleTaskMessage(RedisMessageQueue.TaskMessage message) {
        String executionId = message.getExecutionId();
        if (StringUtils.isEmpty(executionId)) {
            executionId = StringUtils.isNotEmpty(message.getTraceId()) ? message.getTraceId() : message.getTaskId();
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("[EXEC_MSG_RCV] 收到任务 | Job: {} | ExecId: {} | Node: {}",
                    message.getTaskId(), executionId, message.getTargetNode());
            }
            
            // Check for duplicate execution of the same INSTANCE
            if (executingTasks.containsKey(executionId)) {
                log.warn("[EXEC_SKIP] 任务实例 {} (Job: {}) 已在执行中", executionId, message.getTaskId());
                return;
            }

            // 同步执行任务，确保任务执行完成后才返回，从而触发外部的ACK
            try {
                executingTasks.put(executionId, System.currentTimeMillis());

                SysJob sysJob = null;
                try {
                    sysJob = reconstructSysJob(message);
                } catch (Exception e) {
                    log.error("[EXEC_RECONSTRUCT_FAIL] SysJob重建失败，跳过任务 | Job: {} | ExecId: {}",
                        message.getTaskId(), executionId, e);
                    return; // Return effectively ACKs the message (skipped)
                }

                if (sysJob != null) {
                    if (StringUtils.isEmpty(sysJob.getInvokeTarget())) {
                        log.debug("[EXEC_FILTER] 忽略无效任务消息 (invokeTarget为空) | Job: {}", message.getTaskId());
                        return;
                    }
                    executeSysJob(sysJob);
                } else {
                    executeTask(message.getTaskId());
                }

            } catch (Exception e) {
                log.error("[EXEC_FAIL] 任务执行异常 | Job: {} | ExecId: {}", message.getTaskId(), executionId, e);
                // 抛出异常以触发Requeue
                throw new RuntimeException(e);
            } finally {
                executingTasks.remove(executionId);
            }
            
        } catch (Exception e) {
            log.error("[EXEC_ERROR] 执行任务失败 | Job: {} | ExecId: {}", message.getTaskId(), executionId, e);
            throw e;
        }
    }
    
    private SysJob reconstructSysJob(RedisMessageQueue.TaskMessage message) {
        if (message.getPayload() != null && !message.getPayload().isEmpty()) {
            try {
                SysJob sysJob = JSON.parseObject(JSON.toJSONString(message.getPayload()), SysJob.class);
                if (sysJob != null) {
                    sysJob.setTraceId(message.getTraceId());
                    // Ensure JobId and JobName are available if needed
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
        String invokeTarget = sysJob.getInvokeTarget();

        log.info("[TASK_MONITOR] [EXECUTE_START] 开始执行任务 | Job: {} | TraceId: {} | InvokeTarget: {}", jobName, traceId, invokeTarget);

        try {
            JobInvokeUtil.invokeMethod(sysJob);
            
            long endTime = System.currentTimeMillis();
            log.info("[TASK_MONITOR] [EXECUTE_END] 任务执行成功 | Job: {} | TraceId: {} | Cost: {}ms", jobName, traceId, (endTime - startTime));
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("[TASK_MONITOR] [EXECUTE_ERROR] 任务执行失败 | Job: {} | TraceId: {} | Cost: {}ms", jobName, traceId, (endTime - startTime), e);
            throw e;
        }
    }

    private void executeTask(String taskId) throws Exception {
        log.info("[TASK_MONITOR] [EXEC_LEGACY] 使用旧方式执行任务: {}", taskId);

        if (taskId == null) {
             log.warn("[EXEC_INVALID_ID] TaskID为空");
             return;
        }

        // Try to handle both dot and underscore format if legacy
        String separator = taskId.contains("_") ? "_" : "\\.";

        // Defensive: Strict validation of TaskID format
        if (!taskId.contains(separator.replace("\\", "")) && !taskId.contains(".")) {
            // Allow if just jobName or ID? No, safer to require format.
            // Actually, if separator is not present, splits length < 2
        }

        try {
            String[] parts = taskId.split(separator, 2);
            if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                log.warn("[EXEC_INVALID_ID] TaskID解析后格式无效: {}", taskId);
                return;
            }

            // Assumption: First part is Job ID or Group, Second is Name
            // Legacy was group.name. New is jobId_jobName.
            // SysJob needs group/name for logging?
            // Actually executeTask looks up by JobKey(name, group).
            // If new format is jobId_jobName, we might fail to find Quartz Job if we treat jobId as Group.
            // But this method is only a fallback when Payload is missing.
            // Given new distribution always sends Payload, this is purely legacy support.

            String part1 = parts[0];
            String part2 = parts[1];

            // Try to use part1 as Group and part2 as Name (Old behavior)
            String jobGroup = part1;
            String jobName = part2;

            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);

            if (jobDetail == null) {
                log.warn("未找到任务详情 (Legacy Lookup): {}.{}", jobGroup, jobName);
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
        } catch (Exception e) {
            log.error("[EXEC_PARSE_FAIL] 任务参数解析或加载失败 | TaskID: {}", taskId, e);
            // Swallowing exception to prevent thread pool impact, treating as skipped
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
    
    public static boolean isTaskExecuting(String executionId) {
        return executingTasks.containsKey(executionId);
    }
}
