package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.StringUtils;
import com.make.quartz.config.RedisQuartzSemaphore;
import com.make.quartz.domain.SysJob;
import com.make.quartz.domain.SysJobLog;
import com.make.quartz.service.ISysJobLogService;
import com.make.quartz.config.IpBlackListManager;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.make.common.utils.spring.SpringUtils;
import com.make.common.constant.Constants;
import com.make.common.utils.ip.IpUtils;
import com.make.common.util.TraceIdUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 任务分发器
 * 负责任务的全局队列分发和消费
 */
@Component
public class TaskDistributor {

    private static final Logger log = LoggerFactory.getLogger(TaskDistributor.class);

    private static final String GLOBAL_TASK_QUEUE = "GLOBAL_TASK_QUEUE";

    // Redis key for node usage
    private static final String SCHEDULER_NODE_PREFIX = "SCHEDULER_NODE:";
    private static final String SCHEDULER_NODE_USAGE_SUFFIX = ":USAGE";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisQuartzSemaphore redisQuartzSemaphore;

    @Autowired
    private IpBlackListManager ipBlackListManager;

    private ExecutorService consumerExecutor;

    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        // Start the consumer thread
        consumerExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "GlobalTaskConsumer"));
        consumerExecutor.submit(this::consumeTasks);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("TaskDistributor stopped");
    }

    /**
     * 判断任务是否应该在当前节点执行
     *
     * @param jobKey 任务Key
     * @param loadThreshold 负载阈值
     * @return true-应该在当前节点执行，false-应该分发
     */
    public boolean shouldExecuteLocally(String jobKey, double loadThreshold) {
        try {
            // 获取当前节点负载
            String currentNodeId = NodeRegistry.getCurrentNodeId();
            String usageKey = SCHEDULER_NODE_PREFIX + currentNodeId + SCHEDULER_NODE_USAGE_SUFFIX;
            String usageStr = redisTemplate.opsForValue().get(usageKey);
            double currentNodeUsage = usageStr != null ? Double.parseDouble(usageStr) : 0.0;

            if (currentNodeUsage < loadThreshold) {
                return true;
            }

            log.info("当前节点 {} 负载较高 ({}), 任务 {} 将被分发", currentNodeId, currentNodeUsage, jobKey);
            return false;
        } catch (Exception e) {
            log.error("检查本地执行条件失败，默认本地执行", e);
            return true;
        }
    }

    /**
     * 将任务分发到全局队列
     *
     * @param sysJob 任务信息
     */
    public void distributeTask(SysJob sysJob) {
        try {
            // 捕获当前TraceId并设置到sysJob中（如果调用方没有设置）
            if (StringUtils.isEmpty(sysJob.getTraceId())) {
                String traceId = TraceIdUtil.getTraceId();
                if (StringUtils.isNotEmpty(traceId)) {
                    sysJob.setTraceId(traceId);
                }
            }

            String json = JSON.toJSONString(sysJob);
            redisTemplate.opsForList().leftPush(GLOBAL_TASK_QUEUE, json);
            log.info("任务 {} 已推送到全局队列, TraceId: {}", sysJob.getJobName(), sysJob.getTraceId());
        } catch (Exception e) {
            log.error("任务分发失败: {}", sysJob.getJobName(), e);
        }
    }

    /**
     * 消费全局队列中的任务
     */
    private void consumeTasks() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞式获取任务
                String json = redisTemplate.opsForList().rightPop(GLOBAL_TASK_QUEUE, 5, TimeUnit.SECONDS);
                if (StringUtils.isEmpty(json)) {
                    continue;
                }

                SysJob sysJob = JSON.parseObject(json, SysJob.class);
                if (sysJob == null) {
                    continue;
                }

                // 检查IP黑名单
                if (ipBlackListManager.isCurrentNodeIpBlacklisted()) {
                     // 如果当前节点被拉黑，将任务放回队列（右边进，保证顺序或者重新调度）
                     redisTemplate.opsForList().leftPush(GLOBAL_TASK_QUEUE, json);
                     TimeUnit.SECONDS.sleep(5);
                     continue;
                }

                processTask(sysJob);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("全局任务消费异常", e);
            }
        }
    }

    private void processTask(SysJob sysJob) {
        String jobKey = sysJob.getJobGroup() + "." + sysJob.getJobName();
        String lockKey = "quartz:lock:" + jobKey;
        RLock lock = redisQuartzSemaphore.getLock(lockKey);

        // 设置TraceId：优先使用sysJob中传递的TraceId，如果没有则生成新的
        String traceId = sysJob.getTraceId();
        if (StringUtils.isEmpty(traceId)) {
            traceId = TraceIdUtil.generateTraceId();
        }
        TraceIdUtil.putTraceId(traceId);

        boolean locked = false;
        try {
            // 尝试获取锁，设置5秒等待时间，以解决生产者先推队列后释放锁的竞态条件
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                log.info("任务 {} 正在运行中（或无法获取锁），跳过执行", jobKey);
                return;
            }

            log.info("从全局队列获取任务并执行: {}", jobKey);

            long startTime = System.currentTimeMillis();
            try {
                JobInvokeUtil.invokeMethod(sysJob);
                long runMs = System.currentTimeMillis() - startTime;
                recordLog(sysJob, null, runMs);
            } catch (Exception e) {
                long runMs = System.currentTimeMillis() - startTime;
                recordLog(sysJob, e, runMs);
            }

        } catch (Exception e) {
            log.error("执行分发任务失败: {}", jobKey, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            TraceIdUtil.clearTraceId();
        }
    }

    private void recordLog(SysJob sysJob, Exception e, Long runMs) {
        try {
            SysJobLog sysJobLog = new SysJobLog();
            sysJobLog.setJobName(sysJob.getJobName());
            sysJobLog.setJobGroup(sysJob.getJobGroup());
            sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
            sysJobLog.setHostIp(IpUtils.getHostIp());
            sysJobLog.setStartTime(new Date(System.currentTimeMillis() - runMs));

            if (runMs != null) {
                sysJobLog.setStopTime(new Date());
                sysJobLog.setJobMessage(sysJob.getJobName() + " (分布式执行) 总共耗时：" + runMs + "毫秒");
                if (e != null) {
                    sysJobLog.setStatus(Constants.FAIL);
                    sysJobLog.setExceptionInfo(StringUtils.substring(e.getMessage(), 0, 2000));
                } else {
                    sysJobLog.setStatus(Constants.SUCCESS);
                }
            }

            SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
        } catch (Exception ex) {
            log.error("记录任务日志失败", ex);
        }
    }
}
