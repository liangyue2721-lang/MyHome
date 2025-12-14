package com.make.quartz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务监控服务
 * 用于监控任务执行情况，及时发现和报警漏执行的任务
 */
@Service
public class TaskMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(TaskMonitoringService.class);

    private static final String TASK_MONITOR_PREFIX = "TASK_MONITOR:";
    private static final String TASK_EXECUTION_RECORD = "TASK_EXECUTION_RECORD:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void init() {
        // 初始化定时任务监控线程池
        scheduledExecutorService = Executors.newScheduledThreadPool(2);

        // 启动定时检查任务
        scheduledExecutorService.scheduleWithFixedDelay(this::checkMissedTasks, 60, 300, TimeUnit.SECONDS);

        log.info("任务监控服务初始化完成");
    }

    /**
     * 记录任务开始执行
     * @param taskId 任务ID
     */
    public void recordTaskStart(String taskId) {
        try {
            String key = TASK_MONITOR_PREFIX + taskId;
            String startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            redisTemplate.opsForValue().set(key, startTime, 3600, TimeUnit.SECONDS); // 1小时过期
            log.debug("记录任务开始执行: {}", taskId);
        } catch (Exception e) {
            log.error("记录任务开始执行失败: {}", taskId, e);
        }
    }

    /**
     * 记录任务执行完成
     * @param taskId 任务ID
     */
    public void recordTaskComplete(String taskId) {
        try {
            String key = TASK_MONITOR_PREFIX + taskId;
            redisTemplate.delete(key);
            log.debug("记录任务执行完成: {}", taskId);
        } catch (Exception e) {
            log.error("记录任务执行完成失败: {}", taskId, e);
        }
    }

    /**
     * 检查漏执行的任务
     */
    private void checkMissedTasks() {
        try {
            log.info("开始检查漏执行的任务");
            // 这里可以实现具体的漏执行任务检查逻辑
            // 例如：检查Redis中长时间未完成的任务，或者检查任务日志中的异常模式
        } catch (Exception e) {
            log.error("检查漏执行任务时发生异常", e);
        }
    }

    /**
     * 发送告警通知
     * @param message 告警信息
     */
    private void sendAlert(String message) {
        try {
            // 这里可以实现具体的告警通知逻辑
            // 例如：发送邮件、短信或调用告警接口
            log.warn("任务执行告警: {}", message);
        } catch (Exception e) {
            log.error("发送告警通知失败", e);
        }
    }

    /**
     * 关闭监控服务
     */
    public void shutdown() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("任务监控服务已关闭");
        }
    }
}
