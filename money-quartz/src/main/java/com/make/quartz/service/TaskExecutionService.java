package com.make.quartz.service;

import com.make.common.utils.ThreadPoolUtil;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行服务
 * 用于处理从Redis消息队列接收到的任务消息
 */
@Service
public class TaskExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);
    
    @Autowired
    private Scheduler scheduler;
    
    /**
     * 任务执行线程池
     */
    private ThreadPoolExecutor taskExecutor;
    
    /**
     * 用于跟踪正在执行的任务
     * key: taskId, value: 开始执行时间
     */
    private static final ConcurrentHashMap<String, Long> executingTasks = new ConcurrentHashMap<>();
    
    /**
     * 初始化方法，启动消息监听器
     */
    @PostConstruct
    public void init() {
        // 初始化任务执行线程池
        // 优化线程池配置以提升任务执行速度
        this.taskExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,  // 增加核心线程数
                Runtime.getRuntime().availableProcessors() * 8,  // 增加最大线程数
                10000,  // 增加队列容量
                "TaskExecutionThread"
        );
        
        // 获取当前节点ID
        String currentNodeId = SchedulerManager.getCurrentNodeId();
        log.info("初始化任务执行服务，当前节点ID: {}", currentNodeId);
        
        // 启动Redis消息队列监听器
        RedisMessageQueue messageQueue = RedisMessageQueue.getInstance();
        messageQueue.startListening(currentNodeId, this::handleTaskMessage);
        
        log.info("任务执行服务初始化完成，已启动消息监听器，任务执行线程池: 核心线程{}，最大线程{}", 
                Runtime.getRuntime().availableProcessors() * 2,
                Runtime.getRuntime().availableProcessors() * 8);
    }
    
    /**
     * 销毁方法，停止消息监听器
     */
    @PreDestroy
    public void destroy() {
        log.info("开始销毁任务执行服务");
        
        // 停止Redis消息队列监听器
        RedisMessageQueue messageQueue = RedisMessageQueue.getInstance();
        messageQueue.stopListening();
        
        // 关闭任务执行线程池
        if (taskExecutor != null && !taskExecutor.isShutdown()) {
            log.info("正在停止任务执行线程池，当前活跃线程数: {}", taskExecutor.getActiveCount());
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("任务执行线程池未能在5秒内正常关闭，强制关闭");
                    taskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待任务执行线程池关闭时被中断，强制关闭");
                taskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("任务执行线程池已停止");
        }
        
        log.info("任务执行服务销毁完成");
    }
    
    /**
     * 处理任务消息
     * @param message 任务消息
     */
    private void handleTaskMessage(RedisMessageQueue.TaskMessage message) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始处理任务消息，任务ID: {}, 来源节点: {}, 消息时间戳: {}", 
                    message.getTaskId(), message.getTargetNode(), message.getTimestamp());
            
            // 检查任务是否已经在执行
            if (executingTasks.containsKey(message.getTaskId())) {
                log.warn("任务 {} 已在执行中，跳过重复执行", message.getTaskId());
                return;
            }
            
            // 检查任务是否正在Redis消息队列中处理
            if (RedisMessageQueue.isMessageProcessing(message.getTaskId())) {
                log.warn("任务 {} 正在Redis消息队列中处理，跳过重复执行", message.getTaskId());
                return;
            }
            
            // 将任务执行提交到专门的线程池中处理
            taskExecutor.submit(() -> {
                try {
                    // 标记任务为正在执行
                    executingTasks.put(message.getTaskId(), System.currentTimeMillis());
                    log.info("任务 {} 标记为正在执行", message.getTaskId());
                    
                    // 实际执行任务
                    executeTask(message.getTaskId());
                } catch (Exception e) {
                    log.error("执行任务时发生异常，任务ID: {}", message.getTaskId(), e);
                } finally {
                    // 从执行中任务列表中移除
                    executingTasks.remove(message.getTaskId());
                    log.info("任务 {} 从执行中列表移除", message.getTaskId());
                }
            });
            
            long endTime = System.currentTimeMillis();
            log.info("任务消息处理结束，任务ID: {}, 总耗时: {}ms", message.getTaskId(), (endTime - startTime));
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("处理任务消息失败，任务ID: {}, 处理耗时: {}ms", message.getTaskId(), (endTime - startTime), e);
        }
    }
    
    /**
     * 执行具体任务
     * @param taskId 任务ID (格式为: "group.name")
     */
    private void executeTask(String taskId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始执行任务: {}", taskId);
            
            // 解析任务ID获取组名和任务名
            if (taskId == null || !taskId.contains(".")) {
                log.error("任务ID格式不正确: {}", taskId);
                return;
            }
            
            String[] parts = taskId.split("\\.", 2);
            if (parts.length != 2) {
                log.error("任务ID格式不正确，无法解析组名和任务名: {}", taskId);
                return;
            }
            
            String jobGroup = parts[0];
            String jobName = parts[1];
            
            log.info("解析任务信息 - 组名: {}, 任务名: {}", jobGroup, jobName);
            
            // 构造JobKey
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            
            // 从调度器中获取任务详情
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail == null) {
                log.error("未找到任务详情，任务ID: {}", taskId);
                return;
            }
            
            // 获取任务配置信息
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            SysJob sysJob = new SysJob();
            Object jobProperties = jobDataMap.get("TASK_PROPERTIES");
            if (jobProperties != null) {
                // 复制任务属性
                copyBeanProp(sysJob, jobProperties);
            } else {
                log.warn("任务配置信息为空，任务ID: {}", taskId);
            }
            
            // 执行任务
            log.info("开始调用任务执行方法，任务名称: {}", sysJob.getJobName());
            JobInvokeUtil.invokeMethod(sysJob);
            
            long endTime = System.currentTimeMillis();
            log.info("任务 {} 执行完成，执行耗时: {}ms", taskId, (endTime - startTime));
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("执行任务失败，任务ID: {}, 执行耗时: {}ms", taskId, (endTime - startTime), e);
        }
    }
    
    /**
     * 复制Bean属性
     * @param dest 目标对象
     * @param src 源对象
     */
    private void copyBeanProp(Object dest, Object src) {
        try {
            // 使用反射复制属性
            java.lang.reflect.Field[] fields = src.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    // 跳过静态字段和final字段
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                        java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        continue;
                    }
                    
                    field.setAccessible(true);
                    Object value = field.get(src);
                    if (value != null) {
                        java.lang.reflect.Field destField = dest.getClass().getDeclaredField(field.getName());
                        // 跳过目标对象中的静态字段和final字段
                        if (java.lang.reflect.Modifier.isStatic(destField.getModifiers()) || 
                            java.lang.reflect.Modifier.isFinal(destField.getModifiers())) {
                            continue;
                        }
                        destField.setAccessible(true);
                        destField.set(dest, value);
                    }
                } catch (Exception e) {
                    log.warn("复制属性失败，字段名: {}", field.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("复制Bean属性时发生异常", e);
        }
    }
    
    /**
     * 检查任务是否正在执行
     * @param taskId 任务ID
     * @return true-正在执行，false-未在执行
     */
    public static boolean isTaskExecuting(String taskId) {
        return executingTasks.containsKey(taskId);
    }
}