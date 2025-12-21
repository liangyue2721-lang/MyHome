package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.make.common.util.TraceIdUtil;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.domain.SysJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务分发器 (Task Distributor)
 *
 * 核心职责：
 * 1. 监听全局任务队列 (GLOBAL_TASK_QUEUE)
 * 2. 根据节点负载情况，选择最佳节点 (Load Balancing)
 * 3. 将任务分发到目标节点的私有队列 (Priority Queue)
 *
 * 增强日志说明：
 * - [DIST_FETCH]: 从全局队列获取任务
 * - [DIST_SCORING]: 节点评分详情 (DEBUG)
 * - [DIST_DECISION]: 分发决策结果 (Target Node, Priority)
 */
@Component
public class TaskDistributor implements org.springframework.context.SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TaskDistributor.class);

    private static final String GLOBAL_TASK_QUEUE = "task:queue:global";
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:";
    private static final String SCHEDULER_NODE_METRICS_SUFFIX = ":METRICS";

    private final RedisTemplate<String, String> redisTemplate;
    private final SchedulerManager schedulerManager;
    private final RedisMessageQueue redisMessageQueue;
    private final QuartzProperties quartzProperties;

    private ThreadPoolExecutor distributorExecutor;
    private volatile boolean running = false;
    private final java.util.concurrent.ScheduledExecutorService monitorExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public TaskDistributor(RedisTemplate<String, String> redisTemplate,
                          SchedulerManager schedulerManager,
                          RedisMessageQueue redisMessageQueue,
                          QuartzProperties quartzProperties) {
        this.redisTemplate = redisTemplate;
        this.schedulerManager = schedulerManager;
        this.redisMessageQueue = redisMessageQueue;
        this.quartzProperties = quartzProperties;
    }

    @PostConstruct
    public void init() {
        // 使用 Producer Thread Pool 配置
        this.distributorExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                quartzProperties.getProducerCoreSize(),
                quartzProperties.getProducerMaxSize(),
                quartzProperties.getProducerQueueCapacity(),
                "TaskSchedulerProducer"
        );
        this.monitorExecutor.scheduleAtFixedRate(this::monitor, 60, 60, TimeUnit.SECONDS);
        // 定期从全局队列拉取任务 (避免启动时死循环线程的Race Condition)
        this.monitorExecutor.scheduleAtFixedRate(this::processGlobalQueueTasks, 10, 60, TimeUnit.SECONDS);

        log.info("任务分发器初始化完成 | Producer Pool: Core={}, Max={}, Queue={}",
                quartzProperties.getProducerCoreSize(),
                quartzProperties.getProducerMaxSize(),
                quartzProperties.getProducerQueueCapacity());
    }

    @PreDestroy
    public void destroy() {
        this.running = false;
        if (distributorExecutor != null) {
            distributorExecutor.shutdownNow();
        }
        if (monitorExecutor != null) {
            monitorExecutor.shutdownNow();
        }
    }

    /**
     * 定期处理全局队列任务
     * 策略：只要线程池有容量，就循环拉取并提交任务
     */
    private void processGlobalQueueTasks() {
        // 1. 基础检查
        if (!running || !checkIsMaster()) {
            return;
        }

        try {
            // 2. 循环拉取，直到队列满或无任务
            while (distributorExecutor.getQueue().remainingCapacity() > 0) {
                // 使用非阻塞 Pop
                String jobJson = redisTemplate.opsForList().rightPop(GLOBAL_TASK_QUEUE);
                if (StringUtils.isEmpty(jobJson)) {
                    break; // 队列为空
                }

                log.debug("[DIST_FETCH] 从全局队列获取任务");
                // 提交到线程池执行 (标记为来自 Global Queue)
                distributorExecutor.submit(() -> {
                    try {
                        distributeTask(jobJson, true);
                    } catch (Exception ex) {
                        log.error("[DIST_TASK_ERROR] 任务分发执行异常", ex);
                    }
                });
            }
        } catch (Exception e) {
            log.error("[DIST_ERROR] 处理全局队列任务异常", e);
        }
    }

    private boolean checkIsMaster() {
        return true;
    }

    /**
     * 手动分发任务 (供AbstractScheduledTask调用，默认为新任务)
     */
    public void distributeTask(SysJob sysJob) {
        distributeTask(sysJob, false);
    }

    /**
     * 分发任务核心逻辑
     * @param fromGlobalQueue 是否来自全局队列 (如果是，则视为Transfer，跳过重复性检查的SADD返回0判断)
     */
    public void distributeTask(SysJob sysJob, boolean fromGlobalQueue) {
        if (sysJob == null) return;

        if (log.isDebugEnabled()) {
            log.debug("[DIST_ENTRY] 收到任务分发请求 | JobName: {} | FromGlobal: {}", sysJob.getJobName(), fromGlobalQueue);
        }

        // Task ID Format Validation
        if (sysJob.getJobId() == null || StringUtils.isEmpty(sysJob.getJobName())) {
             log.warn("[DIST_INVALID_ID] 任务ID格式无效 (JobId or JobName is empty) | JobName: {}", sysJob.getJobName());
             return;
        }

        try {
            String priority = StringUtils.defaultIfEmpty(sysJob.getPriority(), "NORMAL");
            String traceId = sysJob.getTraceId();

            // Task ID defines "What the task is" (Job Definition)
            // Use underscore as separator
            String taskId = sysJob.getJobId() + "_" + sysJob.getJobName();

            // Execution ID defines "Which instance this is" (Unique per run)
            String executionId = sysJob.getFireInstanceId();

            // 确保TraceId存在
            if (StringUtils.isEmpty(traceId)) {
                traceId = TraceIdUtil.getTraceId(); // 尝试获取当前上下文
                if (StringUtils.isEmpty(traceId)) {
                    traceId = TraceIdUtil.generateTraceId();
                }
                sysJob.setTraceId(traceId);
            }

            // 如果 executionId 为空，使用 traceId 替代
            if (StringUtils.isEmpty(executionId)) {
                executionId = traceId;
            }

            // 选择最佳节点
            String targetNode = selectBestNode(sysJob);

            if (StringUtils.isNotEmpty(targetNode)) {
                log.info("[DIST_DECISION] 任务分发决策 | Job: {} | ExecId: {} | Target: {} | Priority: {} | TraceId: {} | FromGlobal: {}",
                        taskId, executionId, targetNode, priority, traceId, fromGlobalQueue);

                Map<String, Object> payload = JSON.parseObject(JSON.toJSONString(sysJob), Map.class);

                // 发送消息：如果是从GlobalQueue来的，不需要再次检查唯一性(checkUniqueness=false)
                // 否则(新任务)，需要检查唯一性(checkUniqueness=true)
                // 传递 executionId 作为去重键
                redisMessageQueue.sendTaskMessage(
                        taskId,
                        executionId,
                        targetNode,
                        sysJob,
                        payload,
                        sysJob.getTraceId(),
                        priority,
                        !fromGlobalQueue
                );
            } else {
                log.warn("[DIST_NO_NODE] 无可用节点分发任务，准备回退到全局队列 | Job: {} | ExecId: {} | TraceId: {}",
                        taskId, executionId, traceId);
                log.info("[DIST_FALLBACK] 任务回退到全局队列 | Job: {} | ExecId: {}", taskId, executionId);
                // 回退策略：放入全局队列

                // 如果是新任务(!fromGlobalQueue)，需要先尝试添加到Set
                // 使用 executionId 作为去重键
                if (!fromGlobalQueue) {
                    Long added = redisTemplate.opsForSet().add(RedisMessageQueue.PENDING_TASKS_SET, executionId);
                    if (added != null && added == 0) {
                        log.warn("[DIST_DUPLICATE] 任务实例 {} (Job: {}) 已在 Pending 队列中，忽略回退入队", executionId, taskId);
                        return;
                    }
                } else {
                    // 如果是从GlobalQueue出来的，它已经在Set里了，或者因为某些原因不在但我们想保留它
                    // 确保它在Set中
                    redisTemplate.opsForSet().add(RedisMessageQueue.PENDING_TASKS_SET, executionId);
                }

                String jobJson = JSON.toJSONString(sysJob);
                redisTemplate.opsForList().leftPush(GLOBAL_TASK_QUEUE, jobJson);
            }
        } catch (Exception e) {
            log.error("[DIST_FAIL] 分发任务失败: {}", sysJob.getJobName(), e);
            // 异常情况下，如果是新任务且已添加到Set，可能需要清理？
            // 实际上这里的异常大多是Redis连接或序列化，如果Redis挂了，Set操作可能也没成功。
            // 简单起见，暂不回滚Set，依靠过期或清理机制（目前Set没过期，未来可能需要加TTL）
        }
    }

    private void distributeTask(String jobJson) {
        distributeTask(jobJson, false);
    }

    private void distributeTask(String jobJson, boolean fromGlobalQueue) {
        try {
            SysJob sysJob = QuartzBeanUtils.fromJson(jobJson, SysJob.class);
            if (sysJob == null) return;
            distributeTask(sysJob, fromGlobalQueue);
        } catch (Exception e) {
            log.error("[DIST_FAIL] 分发任务失败 (JSON parse error): {}", StringUtils.substring(jobJson, 0, 100), e);
            // 尝试重新入队，防止丢消息
            try {
                redisTemplate.opsForList().leftPush(GLOBAL_TASK_QUEUE, jobJson);
            } catch (Exception ex) {
                log.error("[DIST_FATAL] 任务重新入队失败，消息可能丢失", ex);
            }
        }
    }

    private String selectBestNode(SysJob sysJob) {
        if ("1".equals(sysJob.getIsMasterNode())) {
            log.debug("[DIST_SCORING] 任务指定主节点执行");
            return NodeRegistry.getCurrentNodeId();
        }

        List<String> nodes = schedulerManager.getAliveNodes();
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        String bestNode = null;
        double minScore = Double.MAX_VALUE;

        StringBuilder scoreLog = new StringBuilder();
        if (log.isDebugEnabled()) {
            scoreLog.append("[DIST_SCORING] 节点评分: ");
        }

        for (String nodeId : nodes) {
            double score = calculateNodeScore(nodeId);

            if (log.isDebugEnabled()) {
                scoreLog.append(String.format("[%s: %.2f] ", nodeId, score));
            }

            if (score < minScore) {
                minScore = score;
                bestNode = nodeId;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(scoreLog.toString());
        }

        return bestNode;
    }

    private double calculateNodeScore(String nodeId) {
        try {
            String metricsJson = redisTemplate.opsForValue().get(SCHEDULER_NODE_PREFIX + nodeId + SCHEDULER_NODE_METRICS_SUFFIX);
            if (StringUtils.isEmpty(metricsJson)) {
                return 100.0; // 惩罚分
            }

            Map<String, Object> metrics = JSON.parseObject(metricsJson, Map.class);
            if (metrics == null) return 100.0;

            Double memoryUsage = metrics.containsKey("memoryUsage") ? Double.parseDouble(metrics.get("memoryUsage").toString()) : 0.5;
            Double systemLoad = metrics.containsKey("systemLoad") ? Double.parseDouble(metrics.get("systemLoad").toString()) : 0.5;

            // 评分公式 (越低越好): Memory (40%) + Load (40%) + Random (20%)
            return (memoryUsage * 40) + (systemLoad * 40) + (Math.random() * 20);
        } catch (Exception e) {
            return 100.0;
        }
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * 判断是否应该本地执行
     * (兼容旧接口，实际上现在都通过队列分发)
     */
    public boolean shouldExecuteLocally(String taskId, double threshold) {
        // 返回false表示"本地不执行"，从而触发AbstractScheduledTask调用distributeTask
        return false;
    }

    private void monitor() {
        if (!running) return;
        try {
            log.info("[MONITOR] Pool: TaskDistributor | Active: {}/{} | Queue: {}",
                   distributorExecutor.getActiveCount(), distributorExecutor.getCorePoolSize(), distributorExecutor.getQueue().size());
        } catch (Exception e) {
            log.error("[MONITOR_ERROR] 分发器监控采集失败", e);
        }
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        log.info("停止任务分发器...");
        this.running = false;
        if (distributorExecutor != null) {
            distributorExecutor.shutdownNow();
            try {
                distributorExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
