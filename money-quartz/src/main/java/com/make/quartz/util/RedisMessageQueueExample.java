package com.make.quartz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Redis消息队列使用示例
 * 演示如何使用Redis消息队列在分布式节点间传递任务执行消息
 *
 * 注意：这个示例仅用于演示目的，在实际项目中，任务分发由TaskDistributor类处理
 */
@Component
public class RedisMessageQueueExample implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueueExample.class);

    @Override
    public void run(String... args) throws Exception {
        log.info("Redis消息队列功能说明:");
        log.info("1. 在实际项目中，任务分发由TaskDistributor类自动处理");
        log.info("2. 当主节点负载过高时，TaskDistributor会自动将任务分发给负载较低的节点");
        log.info("3. 节点间通过Redis消息队列传递任务执行消息");
        log.info("4. 每个节点监听自己的任务队列，并执行分配给自己的任务");
        log.info("5. 此示例仅用于演示API用法，实际项目中不需要手动发送任务消息");
        
        /*
         * 实际项目中的任务分发流程如下:
         *
         * 1. Quartz触发任务执行
         * 2. AbstractQuartzJob.execute()方法被调用
         * 3. 调用TaskDistributor.shouldExecuteLocally()决定任务是否在当前节点执行
         * 4. 如果是主节点且负载过高，会选择一个负载较低的节点执行任务
         * 5. 通过RedisMessageQueue.sendTaskMessage()发送任务消息给目标节点
         * 6. 目标节点从自己的队列中获取任务消息并执行
         *
         * 示例代码（实际项目中的用法）:
         *
         * // 获取Redis消息队列实例
         * RedisMessageQueue messageQueue = RedisMessageQueue.getInstance();
         *
         * // 实际项目中，任务ID是Quartz任务的唯一标识
         * String taskId = "任务的唯一标识";
         *
         * // 目标节点ID从SchedulerManager.getCurrentNodeId()获取
         * String targetNode = SchedulerManager.getCurrentNodeId();
         *
         * // 发送任务消息到指定节点
         * messageQueue.sendTaskMessage(taskId, targetNode, jobData);
         *
         * // 节点监听自己的队列并处理任务消息
         * messageQueue.startListening(currentNodeId, message -> {
         *     // 处理接收到的任务消息
         *     handleTaskMessage(message);
         * });
         *
         * // 实际的任务处理逻辑在TaskExecutionService中实现
         */
        
        log.info("请参考TaskDistributor和TaskExecutionService类了解实际的任务分发和执行机制");
    }
    
    /**
     * 处理任务消息的示例方法（实际实现在TaskExecutionService中）
     * @param message 任务消息
     */
    private void handleTaskMessage(RedisMessageQueue.TaskMessage message) {
        log.info("处理任务消息示例 - 任务ID: {}, 目标节点: {}, 时间戳: {}", 
                message.getTaskId(), message.getTargetNode(), message.getTimestamp());
        // 实际的任务执行逻辑
    }
}
