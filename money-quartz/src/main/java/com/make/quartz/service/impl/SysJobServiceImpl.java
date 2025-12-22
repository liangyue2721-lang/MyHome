package com.make.quartz.service.impl;

import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.mapper.SysJobMapper;
import com.make.quartz.service.ISysJobService;
import com.make.quartz.util.NodeRegistry;
import com.make.quartz.util.RedisMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 定时任务服务实现（Redis-only 调度）
 *
 * <p>核心原则：
 * - 数据库负责“任务定义”
 * - Redis 队列负责“调度与执行”
 * - 本类不再创建 Quartz Trigger / JobDetail
 */
@Service
public class SysJobServiceImpl implements ISysJobService {

    private static final Logger log = LoggerFactory.getLogger(SysJobServiceImpl.class);

    private final SysJobMapper jobMapper;

    public SysJobServiceImpl(SysJobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    /**
     * 系统启动后：
     * - 扫描所有启用任务
     * - 将其“下一次执行时间”统一写入 Redis 延迟队列
     *
     * <p>作用：系统重启自恢复
     */
    @PostConstruct
    public void bootstrapEnqueue() {
        try {
            List<SysJob> jobs = jobMapper.selectJobAll();
            if (jobs == null || jobs.isEmpty()) {
                return;
            }

            for (SysJob job : jobs) {
                if (Objects.equals("0", job.getStatus())) {
                    enqueueNextExecution(job);
                }
            }

            log.info("[JOB_BOOTSTRAP] enqueue enabled jobs size={}", jobs.size());
        } catch (Exception e) {
            log.warn("[JOB_BOOTSTRAP_ERR]", e);
        }
    }

    // ======================= 查询接口 =======================

    @Override
    public List<SysJob> selectJobList(SysJob sysJob) {
        return jobMapper.selectJobList(sysJob);
    }

    @Override
    public SysJob selectJobById(Long jobId) {
        return jobMapper.selectJobById(jobId);
    }

    @Override
    public List<SysJob> selectJobAll() {
        return jobMapper.selectJobAll();
    }

    // ======================= 写接口 =======================

    @Override
    public int insertJob(SysJob job) {
        int rows = jobMapper.insertJob(job);
        if (rows > 0 && Objects.equals("0", job.getStatus())) {
            enqueueNextExecution(job);
        }
        return rows;
    }

    @Override
    public int updateJob(SysJob job) {
        int rows = jobMapper.updateJob(job);
        if (rows > 0 && Objects.equals("0", job.getStatus())) {
            enqueueNextExecution(job);
        }
        return rows;
    }

    @Override
    public int deleteJob(SysJob job) {
        // 只删数据库，Redis 中的历史/待执行消息允许自然过期
        return jobMapper.deleteJobById(job.getJobId());
    }

    @Override
    public int changeStatus(SysJob job) {
        int rows = jobMapper.updateJob(job);
        if (rows > 0 && Objects.equals("0", job.getStatus())) {
            enqueueNextExecution(job);
        }
        return rows;
    }

    // ======================= 工具方法 =======================

    /**
     * 校验 cron 表达式是否合法
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        if (StringUtils.isEmpty(cronExpression)) {
            return false;
        }
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算下一次执行时间并写入 Redis 延迟队列
     *
     * <p>这是“调度生产”的唯一入口
     */
    private void enqueueNextExecution(SysJob job) {
        String cron = job.getCronExpression();
        if (StringUtils.isEmpty(cron)) {
            return;
        }

        try {
            CronExpression cronExpression = CronExpression.parse(cron);
            ZonedDateTime nextTime =
                    cronExpression.next(ZonedDateTime.now(ZoneId.systemDefault()));

            if (nextTime == null) {
                return;
            }

            long nextAtMillis = nextTime.toInstant().toEpochMilli();
            String nodeId = NodeRegistry.getCurrentNodeId();

            RedisMessageQueue.getInstance()
                    .enqueueAt(job, nodeId, "NORMAL", nextAtMillis);

            log.info(
                    "[JOB_ENQUEUE] jobId={} name={} nextAt={}",
                    job.getJobId(),
                    job.getJobName(),
                    nextAtMillis
            );
        } catch (Exception e) {
            log.warn(
                    "[JOB_ENQUEUE_ERR] jobId={} cron={}",
                    job.getJobId(),
                    cron,
                    e
            );
        }
    }
}
