package com.make.quartz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务监控服务
 * 用于监控任务执行情况，发现执行超时 / 漏执行任务并告警
 */
@Service
public class TaskMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(TaskMonitoringService.class);

    /**
     * 任务执行中标记
     */
    private static final String TASK_MONITOR_PREFIX = "TASK_MONITOR:";

    /**
     * 任务告警记录，防止重复告警
     */
    private static final String TASK_EXECUTION_RECORD = "TASK_EXECUTION_RECORD:";

    /**
     * 单个任务最大允许执行时间（秒）
     */
    private static final long TASK_TIMEOUT_SECONDS = 600; // 10分钟

    /**
     * 告警冷却时间（秒）
     */
    private static final long ALERT_COOLDOWN_SECONDS = 1800; // 30分钟

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void init() {
        scheduledExecutorService = Executors.newScheduledThreadPool(2);

        // 每 5 分钟扫描一次
        scheduledExecutorService.scheduleWithFixedDelay(
                this::checkMissedTasks, 60, 300, TimeUnit.SECONDS
        );

        log.info("任务监控服务初始化完成");
    }

    /**
     * 记录任务开始执行
     */
    public void recordTaskStart(String taskId) {
        try {
            String key = TASK_MONITOR_PREFIX + taskId;
            String startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            redisTemplate.opsForValue().set(key, startTime, 3600, TimeUnit.SECONDS);
            log.debug("记录任务开始执行: {}", taskId);
        } catch (Exception e) {
            log.error("记录任务开始执行失败: {}", taskId, e);
        }
    }

    /**
     * 记录任务执行完成
     */
    public void recordTaskComplete(String taskId) {
        try {
            redisTemplate.delete(TASK_MONITOR_PREFIX + taskId);
            redisTemplate.delete(TASK_EXECUTION_RECORD + taskId);
            log.debug("记录任务执行完成: {}", taskId);
        } catch (Exception e) {
            log.error("记录任务执行完成失败: {}", taskId, e);
        }
    }

    /**
     * 检查执行超时 / 漏执行任务
     */
    private void checkMissedTasks() {
        try {
            log.info("开始检查漏执行任务");

            Set<String> keys = redisTemplate.keys(TASK_MONITOR_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            for (String key : keys) {
                String taskId = key.replace(TASK_MONITOR_PREFIX, "");
                String startTimeStr = redisTemplate.opsForValue().get(key);
                if (startTimeStr == null) {
                    continue;
                }

                LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                long runningSeconds = Duration.between(startTime, now).getSeconds();

                if (runningSeconds > TASK_TIMEOUT_SECONDS) {
                    handleTimeoutTask(taskId, runningSeconds);
                }
            }
        } catch (Exception e) {
            log.error("检查漏执行任务异常", e);
        }
    }

    /**
     * 处理超时任务
     */
    private void handleTimeoutTask(String taskId, long runningSeconds) {
        String alertKey = TASK_EXECUTION_RECORD + taskId;

        // 冷却期内不重复告警
        if (Boolean.TRUE.equals(redisTemplate.hasKey(alertKey))) {
            return;
        }

        String message = String.format(
                "任务 [%s] 执行超时，已运行 %d 秒，可能发生卡死或漏执行",
                taskId, runningSeconds
        );

        sendAlert(message);

        // 记录告警
        redisTemplate.opsForValue().set(
                alertKey,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                ALERT_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 发送告警通知
     * 后续可对接：钉钉 / 企业微信 / Prometheus AlertManager
     */
    private void sendAlert(String message) {
        try {
            log.warn("任务执行告警: {}", message);
        } catch (Exception e) {
            log.error("发送告警通知失败", e);
        }
    }

    /**
     * 关闭监控服务
     */
    @PreDestroy
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
