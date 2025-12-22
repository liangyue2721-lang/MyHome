package com.make.quartz.service;

import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.util.JobInvokeUtil;
import com.make.quartz.util.NodeRegistry;
import com.make.quartz.util.RedisMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 队列消费执行服务（Redis-only）
 *
 * <p>职责：
 * 1) 启动当前节点的队列监听（每节点消费自己的队列）
 * 2) 收到消息后执行 SysJob（invokeTarget）
 * 3) 执行成功后：计算下一次触发时间，并重新入队（实现“定时任务”的循环）
 *
 * <p>注意：
 * - Scheduled 时间检查已经在 RedisMessageQueue 内通过 delay zset 实现
 * - 这里仅负责“执行”和“续约下一次”
 */
@Component
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    /**
     * 防止同 executionId 重入
     */
    private final ConcurrentHashMap<String, Long> executing = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String nodeId = NodeRegistry.getCurrentNodeId();
        RedisMessageQueue.getInstance().startListening(nodeId, this::handle);
    }


    @PreDestroy
    public void destroy() {
        RedisMessageQueue.getInstance().stopListening();
    }

    /**
     * 处理一条队列消息
     */
    private void handle(RedisMessageQueue.TaskMessage message) throws Exception {
        String execId = StringUtils.isEmpty(message.getExecutionId())
                ? UUID.randomUUID().toString()
                : message.getExecutionId();

        if (executing.putIfAbsent(execId, System.currentTimeMillis()) != null) {
            log.warn("[EXEC_SKIP] duplicate execId={}, taskId={}", execId, message.getTaskId());
            return;
        }

        try {
            // 时间检查（兜底）：如果还没到 scheduledAt，则直接放回 delay（避免 ready 推进误差）
            long now = System.currentTimeMillis();
            if (message.getScheduledAt() > now) {
                SysJob sj = (SysJob) message.getJobData();
                if (sj != null) {
                    RedisMessageQueue.getInstance().enqueueAt(
                            sj,
                            message.getTargetNode(),
                            message.getPriority(),
                            message.getScheduledAt()
                    );
                }
                return;
            }

            SysJob sysJob = (SysJob) message.getJobData();
            if (sysJob == null || StringUtils.isEmpty(sysJob.getInvokeTarget())) {
                log.warn("[EXEC_DROP] invalid message, taskId={}, execId={}", message.getTaskId(), execId);
                return;
            }

            executeOnce(sysJob);

            // 执行成功后，计算下一次并续入队（实现定时）
            scheduleNextIfNeeded(sysJob, message.getTargetNode(), message.getPriority());

        } finally {
            executing.remove(execId);
        }
    }

    /**
     * 执行一次任务（消费逻辑）
     */
    private void executeOnce(SysJob sysJob) throws Exception {
        log.info("[EXEC] start jobId={} name={} target={}", sysJob.getJobId(), sysJob.getJobName(), sysJob.getInvokeTarget());
        JobInvokeUtil.invokeMethod(sysJob);
        log.info("[EXEC] success jobId={} name={}", sysJob.getJobId(), sysJob.getJobName());
    }

    /**
     * 如果是 cron 任务，计算下一次触发时间并入队（队列内部延迟执行）
     *
     * <p>说明：
     * - 你要求“在队列中嵌入 Scheduled 时间检查”，这里计算 nextAt，
     * 但真正的“是否到期执行”由 RedisMessageQueue 的 delay zset 推进完成。
     */
    private void scheduleNextIfNeeded(SysJob sysJob, String targetNode, String priority) {
        // status=0 才继续（你项目常用：0正常 1暂停）
        if (!Objects.equals("0", sysJob.getStatus())) {
            return;
        }

        String cron = sysJob.getCronExpression();
        if (StringUtils.isEmpty(cron)) {
            return;
        }

        try {
            CronExpression ce = CronExpression.parse(cron);
            ZonedDateTime next = ce.next(ZonedDateTime.now(ZoneId.systemDefault()));
            if (next == null) {
                return;
            }

            long nextAt = next.toInstant().toEpochMilli();

            // 续入队（延迟）
            RedisMessageQueue.getInstance().enqueueAt(sysJob, targetNode, priority, nextAt);

            log.info("[EXEC_NEXT] jobId={} nextAt={}", sysJob.getJobId(), Instant.ofEpochMilli(nextAt));
        } catch (Exception e) {
            log.warn("[EXEC_NEXT_ERR] jobId={} cron={}", sysJob.getJobId(), cron, e);
        }
    }
}
