package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的简易消息队列实现
 * 用于在分布式节点间传递任务执行消息
 */
@Component
public class RedisMessageQueue {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueue.class);

    /**
     * 任务队列Redis key前缀
     */
    private static final String TASK_QUEUE_PREFIX = "task:queue:";

    /**
     * 任务队列Redis key
     */
    private static final String TASK_QUEUE_KEY = "task:queue:jobs";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ThreadPoolExecutor executorService;

    // 添加一个用于任务处理的线程池
    private ThreadPoolExecutor taskProcessExecutor;

    private static RedisMessageQueue instance;

    /**
     * 用于跟踪正在处理的消息
     * key: taskId, value: 开始处理时间
     */
    private static final ConcurrentHashMap<String, Long> processingMessages = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化监听线程池（用于监听Redis队列）
        // 提高监听线程池的核心线程数和最大线程数，以提升任务消费速度
        this.executorService = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                Runtime.getRuntime().availableProcessors(),  // 核心线程数改为CPU核心数
                Runtime.getRuntime().availableProcessors() * 4,  // 最大线程数改为CPU核心数的4倍
                200,  // 增加队列容量
                "RedisQueueListener"
        );

        // 初始化任务处理线程池（用于实际处理任务）
        this.taskProcessExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,  // 增加核心线程数
                Runtime.getRuntime().availableProcessors() * 8,  // 增加最大线程数
                10000,  // 增加队列容量
                "TaskProcessor"
        );

        instance = this;
        log.info("Redis消息队列初始化完成，监听线程池: 核心线程{}，最大线程{}；任务处理线程池: 核心线程{}，最大线程{}",
                Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 4,
                Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 8);
    }

    /**
     * 获取RedisMessageQueue实例
     * @return RedisMessageQueue实例
     */
    public static RedisMessageQueue getInstance() {
        if (instance == null) {
            instance = SpringUtils.getBean(RedisMessageQueue.class);
        }
        return instance;
    }

    /**
     * 发送任务消息到队列
     * @param taskId 任务ID
     * @param targetNode 目标节点
     * @param jobData 任务数据
     */
    public void sendTaskMessage(String taskId, String targetNode, Object jobData) {
        try {
            // 检查任务是否已经在处理中
            if (processingMessages.containsKey(taskId)) {
                log.warn("任务 {} 已在处理中，不重复发送消息", taskId);
                return;
            }

            // 检查任务是否正在AbstractQuartzJob中执行
            if (AbstractQuartzJob.isJobExecuting(taskId)) {
                log.warn("任务 {} 正在AbstractQuartzJob中执行，不重复发送消息", taskId);
                return;
            }

            // 检查任务是否已在TaskExecutionService中执行
            if (TaskExecutionService.isTaskExecuting(taskId)) {
                log.warn("任务 {} 正在TaskExecutionService中执行，不重复发送消息", taskId);
                return;
            }

            // 构造消息
            TaskMessage message = new TaskMessage();
            message.setTaskId(taskId);
            message.setTargetNode(targetNode);
            message.setJobData(jobData);
            message.setTimestamp(System.currentTimeMillis());

            // 将消息序列化为JSON
            String messageJson = JSON.toJSONString(message);

            // 记录发送的消息内容，便于调试
            log.debug("准备发送任务消息: {}", messageJson);

            // 发送到Redis队列
            String queueKey = TASK_QUEUE_PREFIX + targetNode;
            Long queueLength = redisTemplate.opsForList().leftPush(queueKey, messageJson);

            log.info("任务消息已发送到队列，任务ID: {}, 目标节点: {}, 队列名称: {}, 队列长度: {}",
                    taskId, targetNode, queueKey, queueLength);
        } catch (Exception e) {
            log.error("发送任务消息失败，任务ID: {}, 目标节点: {}", taskId, targetNode, e);
        }
    }

    /**
     * 启动消息监听器
     * @param nodeId 当前节点ID
     * @param messageHandler 消息处理器
     */
    public void startListening(String nodeId, MessageHandler messageHandler) {
        String queueKey = TASK_QUEUE_PREFIX + nodeId;
        log.info("启动消息监听器，监听队列: {}, 当前节点ID: {}", queueKey, nodeId);

        // 提交监听任务到线程池
        executorService.submit(() -> {
            log.info("消息监听线程已启动，监听队列: {}", queueKey);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 阻塞式获取消息，超时时间为10秒
                    String messageJson = redisTemplate.opsForList().rightPop(queueKey, 10, TimeUnit.SECONDS);
                    if (messageJson != null) {
                        log.debug("接收到原始消息内容: {}", messageJson);

                        // 解析消息
                        TaskMessage message = parseMessage(messageJson);
                        if (message == null) {
                            log.warn("无法解析消息内容，跳过处理: {}", messageJson);
                            continue;
                        }

                        // 检查任务是否已经在处理中
                        if (processingMessages.containsKey(message.getTaskId())) {
                            log.warn("任务 {} 已在处理中，跳过重复处理", message.getTaskId());
                            continue;
                        }

                        // 检查任务是否正在AbstractQuartzJob中执行
                        if (AbstractQuartzJob.isJobExecuting(message.getTaskId())) {
                            log.warn("任务 {} 正在AbstractQuartzJob中执行，跳过重复处理", message.getTaskId());
                            continue;
                        }

                        // 检查任务是否已在TaskExecutionService中执行
                        if (TaskExecutionService.isTaskExecuting(message.getTaskId())) {
                            log.warn("任务 {} 正在TaskExecutionService中执行，跳过重复处理", message.getTaskId());
                            continue;
                        }

                        log.info("接收到任务消息，任务ID: {}, 目标节点: {}, 发送时间: {}",
                                message.getTaskId(), message.getTargetNode(), message.getTimestamp());

                        // 将消息处理任务提交到专门的任务处理线程池
                        taskProcessExecutor.submit(() -> {
                            // 标记消息为正在处理
                            processingMessages.put(message.getTaskId(), System.currentTimeMillis());
                            log.info("任务 {} 标记为正在处理", message.getTaskId());

                            // 处理消息
                            long startTime = System.currentTimeMillis();
                            try {
                                messageHandler.handleMessage(message);
                            } catch (Exception e) {
                                log.error("处理任务消息时发生业务异常，任务ID: {}", message.getTaskId(), e);
                            } finally {
                                // 从处理中消息列表中移除
                                processingMessages.remove(message.getTaskId());
                                log.info("任务 {} 从处理中列表移除", message.getTaskId());

                                long endTime = System.currentTimeMillis();
                                log.info("任务消息处理完成，任务ID: {}, 处理耗时: {}ms", message.getTaskId(), (endTime - startTime));
                            }
                        });
                    } else {
                        log.debug("队列超时未获取到消息，继续监听，队列名称: {}", queueKey);
                    }
                } catch (Exception e) {
                    log.error("处理消息时发生异常，队列名称: {}", queueKey, e);
                    // 防止异常导致监听线程中断
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("消息监听线程被中断，队列名称: {}", queueKey);
                        break;
                    }
                }
            }
            log.info("消息监听线程已退出，队列名称: {}", queueKey);
        });
    }

    /**
     * 停止消息监听
     */
    public void stopListening() {
        if (executorService != null && !executorService.isShutdown()) {
            log.info("正在停止消息监听器，当前活跃线程数: {}", executorService.getActiveCount());
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("消息监听器未能在5秒内正常关闭，强制关闭");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待消息监听器关闭时被中断，强制关闭");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("消息监听器已停止");
        } else {
            log.debug("消息监听器已处于关闭状态，无需重复关闭");
        }

        // 同时关闭任务处理线程池
        if (taskProcessExecutor != null && !taskProcessExecutor.isShutdown()) {
            log.info("正在停止任务处理线程池，当前活跃线程数: {}", taskProcessExecutor.getActiveCount());
            taskProcessExecutor.shutdown();
            try {
                if (!taskProcessExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("任务处理线程池未能在5秒内正常关闭，强制关闭");
                    taskProcessExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待任务处理线程池关闭时被中断，强制关闭");
                taskProcessExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("任务处理线程池已停止");
        } else {
            log.debug("任务处理线程池已处于关闭状态，无需重复关闭");
        }
    }

    /**
     * 解析消息内容
     * @param messageContent 消息内容
     * @return 解析后的TaskMessage对象，解析失败返回null
     */
    private TaskMessage parseMessage(String messageContent) {
        // 首先尝试按JSON格式解析
        try {
            return JSON.parseObject(messageContent, TaskMessage.class);
        } catch (JSONException e) {
            log.debug("按JSON格式解析消息失败: {}", messageContent);
        }

        // 如果JSON解析失败，尝试按旧格式解析（直接的任务ID）
        try {
            TaskMessage message = new TaskMessage();
            message.setTaskId(messageContent);
            message.setTargetNode(""); // 目标节点未知
            message.setJobData(null);
            message.setTimestamp(System.currentTimeMillis());
            log.info("按旧格式解析消息成功: {}", messageContent);
            return message;
        } catch (Exception e) {
            log.error("按旧格式解析消息也失败: {}", messageContent, e);
        }

        // 两种格式都解析失败
        return null;
    }

    /**
     * 任务消息类
     */
    public static class TaskMessage {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 目标节点
         */
        private String targetNode;

        /**
         * 任务数据
         */
        private Object jobData;

        /**
         * 时间戳
         */
        private long timestamp;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getTargetNode() {
            return targetNode;
        }

        public void setTargetNode(String targetNode) {
            this.targetNode = targetNode;
        }

        public Object getJobData() {
            return jobData;
        }

        public void setJobData(Object jobData) {
            this.jobData = jobData;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "TaskMessage{" +
                    "taskId='" + taskId + '\'' +
                    ", targetNode='" + targetNode + '\'' +
                    ", jobData=" + jobData +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    /**
     * 消息处理器接口
     */
    public interface MessageHandler {
        /**
         * 处理消息
         * @param message 任务消息
         */
        void handleMessage(TaskMessage message);
    }

    /**
     * 检查消息是否正在处理
     * @param taskId 任务ID
     * @return true-正在处理，false-未在处理
     */
    public static boolean isMessageProcessing(String taskId) {
        return processingMessages.containsKey(taskId);
    }
}
