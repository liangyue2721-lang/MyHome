package com.make.quartz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.executor.GenericTaskExecutor;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.MDC;


/**
 * 一个可直接运行的 Redis 消息队列示例。
 * 该类在应用启动时注册当前节点的消息监听器，并发送一批测试任务到本节点。
 * 收到的任务将由 {@link #handleTaskMessage(RedisMessageQueue.TaskMessage)} 方法处理。
 * <p>
 * 使用方式：将本类添加到 Spring Boot 项目中，确保已注入 {@link RedisMessageQueue}，
 * 启动应用后即可看到任务发送与消费的日志。
 */
@Component
public class RedisMessageQueueExample implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueueExample.class);

    /**
     * Redis 消息队列实例，通过 Spring 自动注入。
     */
    @Autowired
    private RedisMessageQueue messageQueue;

    /**
     * 通用任务执行器，用于执行 SysJob 类型的任务
     */
    @Autowired
    private GenericTaskExecutor genericTaskExecutor;

    /**
     * Quartz 调度器，用于根据任务 ID 查询任务详情
     */
    @Autowired
    private Scheduler scheduler;

    @Override
    public void run(String... args) {
        // 获取当前节点 ID，并注册消息监听器
        String currentNodeId = SchedulerManager.getCurrentNodeId();
        messageQueue.startListening(currentNodeId, this::handleTaskMessage);
        log.info("RedisMessageQueueExample 已启动，当前节点 ID: {}，开始监听本节点任务队列", currentNodeId);

        // 发送几条示例任务到本节点进行测试
        sendSampleTasks(currentNodeId, 3);
    }

    /**
     * 向指定节点发送多个测试任务。实际使用时，可以根据业务需求调用此方法。
     *
     * @param targetNodeId 接收任务的目标节点
     * @param count        要发送的任务数量
     */
    private void sendSampleTasks(String targetNodeId, int count) {
        for (int i = 0; i < count; i++) {
            String taskId = UUID.randomUUID().toString();
            Map<String, Object> payload = new HashMap<>();
            payload.put("info", "这是任务 " + taskId + " 的示例数据");
            payload.put("index", i);
            messageQueue.sendTaskMessage(taskId, targetNodeId, payload);
            log.info("已发送示例任务 {} 至节点 {}", taskId, targetNodeId);
        }
    }

    /**
     * 消息处理逻辑。当本节点收到来自 Redis 队列的任务消息时，会调用此方法。
     * 这里的实现简单地打印任务信息，可根据业务需要扩展。
     *
     * @param message 收到的任务消息
     */
    private void handleTaskMessage(RedisMessageQueue.TaskMessage message) {
        log.info("开始处理任务，任务 ID: {}，目标节点: {}，发送时间: {}", message.getTaskId(), message.getTargetNode(), message.getTimestamp());
        Object jobData = message.getJobData();
        SysJob sysJob = null;
        // 优先尝试从 jobData 构造 SysJob
        if (jobData != null) {
            try {
                // 如果 jobData 已经是 SysJob 类型，直接使用
                if (jobData instanceof SysJob) {
                    sysJob = (SysJob) jobData;
                } else {
                    // 将 jobData 对象序列化成 JSON，再解析为 SysJob
                    String json = JSON.toJSONString(jobData);
                    sysJob = JSON.parseObject(json, SysJob.class);
                }
            } catch (Exception e) {
                log.warn("无法从 jobData 构建 SysJob 对象，将尝试根据 taskId 查询任务详情", e);
                sysJob = null;
            }
        }

        try {
            if (sysJob != null) {
                // 设置链路追踪 ID 到 MDC，便于日志追踪
                if (StringUtils.isNotEmpty(sysJob.getTraceId())) {
                    MDC.put("traceId", sysJob.getTraceId());
                }
                // 调用通用任务执行器执行任务
                genericTaskExecutor.execute(sysJob);
                log.info("任务 {}[{}] 执行完成", sysJob.getJobName(), sysJob.getJobId());
            } else {
                // 如果 jobData 无法解析，则通过 taskId (格式: group.name) 查询 Quartz 任务并执行
                String taskId = message.getTaskId();
                if (StringUtils.isEmpty(taskId) || !taskId.contains(".")) {
                    log.error("taskId 格式不正确，无法执行任务: {}", taskId);
                } else {
                    executeTaskByTaskId(taskId);
                }
            }
        } catch (Exception e) {
            log.error("处理任务 {} 失败", message.getTaskId(), e);
        } finally {
            // 清除 MDC 中的 traceId
            MDC.remove("traceId");
        }
        log.info("任务 {} 处理完成", message.getTaskId());
    }

    /**
     * 根据 taskId 查询 Quartz 中的任务并执行。
     * taskId 格式为 "group.name"。
     *
     * @param taskId 任务唯一标识
     */
    private void executeTaskByTaskId(String taskId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("根据 taskId 执行任务: {}", taskId);
            String[] parts = taskId.split("\\.", 2);
            if (parts.length != 2) {
                log.error("taskId 格式不正确，无法解析: {}", taskId);
                return;
            }
            String jobGroup = parts[0];
            String jobName = parts[1];
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail == null) {
                log.error("未找到任务 {}.{} 的 JobDetail", jobGroup, jobName);
                return;
            }
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            Object jobProperties = jobDataMap.get("TASK_PROPERTIES");
            SysJob sysJob = new SysJob();
            // 如果 TASK_PROPERTIES 存在，则将其序列化为 SysJob
            if (jobProperties != null) {
                try {
                    String json = JSON.toJSONString(jobProperties);
                    sysJob = JSON.parseObject(json, SysJob.class);
                } catch (Exception e) {
                    log.warn("无法将 TASK_PROPERTIES 转换为 SysJob，对象类型: {}", jobProperties.getClass(), e);
                }
            } else {
                log.warn("任务 {}.{} 的 JobDataMap 中未找到 TASK_PROPERTIES，使用默认的 SysJob", jobGroup, jobName);
            }
            // 设置任务名和组名，避免为空
            sysJob.setJobGroup(jobGroup);
            sysJob.setJobName(jobName);
            // 调用任务执行工具执行
            log.info("开始调用任务执行方法，任务名称: {}.{}", jobGroup, jobName);
            JobInvokeUtil.invokeMethod(sysJob);
            long endTime = System.currentTimeMillis();
            log.info("任务 {} 执行完成，耗时 {}ms", taskId, (endTime - startTime));
        } catch (SchedulerException e) {
            long endTime = System.currentTimeMillis();
            log.error("执行任务 {} 失败，耗时 {}ms", taskId, (endTime - startTime), e);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("执行任务 {} 时发生异常，耗时 {}ms", taskId, (endTime - startTime), e);
        }
    }
}
