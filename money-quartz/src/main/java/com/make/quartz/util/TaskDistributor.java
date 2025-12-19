package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.make.common.util.TraceIdUtil;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.common.utils.ip.IpUtils;
import com.make.quartz.config.QuartzProperties;
import com.make.quartz.domain.SysJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
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
public class TaskDistributor {

    private static final Logger log = LoggerFactory.getLogger(TaskDistributor.class);

    private static final String GLOBAL_TASK_QUEUE = "task:queue:global";
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:";
    private static final String SCHEDULER_NODE_METRICS_SUFFIX = ":METRICS";

    private final RedisTemplate<String, String> redisTemplate;
    private final SchedulerManager schedulerManager;
    private final RedisMessageQueue redisMessageQueue;
    private final QuartzProperties quartzProperties;

    private ThreadPoolExecutor distributorExecutor;

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
        this.distributorExecutor = (ThreadPoolExecutor) ThreadPoolUtil.createCustomThreadPool(
                quartzProperties.getDistributorThreads(),
                quartzProperties.getDistributorThreads() * 2,
                100,
                "TaskDistributor"
        );

        startDistributor();
        log.info("任务分发器初始化完成 | Threads: {}", quartzProperties.getDistributorThreads());
    }

    @PreDestroy
    public void destroy() {
        if (distributorExecutor != null) {
            distributorExecutor.shutdownNow();
        }
    }

    private void startDistributor() {
        for (int i = 0; i < quartzProperties.getDistributorThreads(); i++) {
            distributorExecutor.submit(this::distributeTasksLoop);
        }
    }

    private void distributeTasksLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!checkIsMaster()) {
                    sleep(5000);
                    continue;
                }

                // 阻塞获取任务
                String jobJson = redisTemplate.opsForList().rightPop(GLOBAL_TASK_QUEUE, 5, TimeUnit.SECONDS);

                if (jobJson != null) {
                    log.debug("[DIST_FETCH] 从全局队列获取任务");
                    distributeTask(jobJson);
                }
            } catch (Exception e) {
                log.error("[DIST_ERROR] 任务分发异常", e);
                sleep(1000);
            }
        }
    }

    private boolean checkIsMaster() {
        return true;
    }

    /**
     * 手动分发任务 (供AbstractScheduledTask调用)
     */
    public void distributeTask(SysJob sysJob) {
        if (sysJob == null) return;

        try {
            String priority = StringUtils.defaultIfEmpty(sysJob.getPriority(), "NORMAL");
            String traceId = sysJob.getTraceId();

            // 确保TraceId存在
            if (StringUtils.isEmpty(traceId)) {
                traceId = TraceIdUtil.getTraceId(); // 尝试获取当前上下文
                if (StringUtils.isEmpty(traceId)) {
                    traceId = TraceIdUtil.generateTraceId();
                }
                sysJob.setTraceId(traceId);
            }

            // 选择最佳节点
            String targetNode = selectBestNode(sysJob);

            if (StringUtils.isNotEmpty(targetNode)) {
                log.info("[DIST_DECISION] 任务分发决策 | Task: {} | Target: {} | Priority: {} | TraceId: {}",
                        sysJob.getJobName(), targetNode, priority, traceId);

                Map<String, Object> payload = JSON.parseObject(JSON.toJSONString(sysJob), Map.class);

                redisMessageQueue.sendTaskMessage(
                        sysJob.getJobId() + "." + sysJob.getJobName(),
                        targetNode,
                        sysJob,
                        payload,
                        sysJob.getTraceId(),
                        priority
                );
            } else {
                log.warn("[DIST_NO_NODE] 无可用节点分发任务: {}", sysJob.getJobName());
                // 回退策略：放入全局队列
                String jobJson = JSON.toJSONString(sysJob);
                redisTemplate.opsForList().leftPush(GLOBAL_TASK_QUEUE, jobJson);
            }
        } catch (Exception e) {
            log.error("[DIST_FAIL] 分发任务失败: {}", sysJob.getJobName(), e);
        }
    }

    private void distributeTask(String jobJson) {
        try {
            SysJob sysJob = QuartzBeanUtils.fromJson(jobJson, SysJob.class);
            if (sysJob == null) return;
            distributeTask(sysJob);
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
}
