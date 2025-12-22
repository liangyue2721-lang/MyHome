package com.make.quartz.util;

import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务分发器（Redis-only 生产侧）
 *
 * <p>职责：
 * - 不再执行任务
 * - 不再创建 Quartz Trigger
 * - 只做一件事：把任务投递到 RedisMessageQueue（立即或延迟）
 *
 * <p>路由策略：
 * - 这里给出最简单策略：选择当前节点（或按你的 NodeRegistry 实现扩展一致性哈希）。
 */
@Component
public class TaskDistributor {

    private static final Logger log = LoggerFactory.getLogger(TaskDistributor.class);

    /**
     * 分发单个任务（立即执行）
     */
    public void distributeNow(SysJob sysJob) {
        String nodeId = NodeRegistry.getCurrentNodeId();
        String priority = "NORMAL";

        log.info("[DIST_NOW] jobId={} node={}", sysJob.getJobId(), nodeId);
        RedisMessageQueue.getInstance().enqueueNow(sysJob, nodeId, priority);
    }

    /**
     * 批量分发（立即执行）
     */
    public void distributeNow(List<SysJob> jobs) {
        if (jobs == null || jobs.isEmpty()) return;
        for (SysJob job : jobs) {
            distributeNow(job);
        }
    }

    /**
     * 分发指定时间执行的任务（延迟执行）
     */
    public void distributeAt(SysJob sysJob, long scheduledAtMillis) {
        String nodeId = NodeRegistry.getCurrentNodeId();
        String priority = "NORMAL";

        log.info("[DIST_AT] jobId={} node={} at={}", sysJob.getJobId(), nodeId, scheduledAtMillis);
        RedisMessageQueue.getInstance().enqueueAt(sysJob, nodeId, priority, scheduledAtMillis);
    }
}
