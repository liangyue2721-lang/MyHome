package com.make.quartz.service.impl;

import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.mapper.SysJobMapper;
import com.make.quartz.service.ISysJobService;
import com.make.quartz.util.TaskDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SysJobServiceImpl implements ISysJobService {

    private static final Logger log = LoggerFactory.getLogger(SysJobServiceImpl.class);

    private final SysJobMapper jobMapper;
    private final TaskDistributor taskDistributor;

    /**
     * 防止同 JVM 内重复 bootstrap
     */
    private final AtomicBoolean bootstrapped = new AtomicBoolean(false);

    public SysJobServiceImpl(SysJobMapper jobMapper, TaskDistributor taskDistributor) {
        this.jobMapper = jobMapper;
        this.taskDistributor = taskDistributor;
    }

    /* ========================= 启动/恢复入口 ========================= */

    public void bootstrapEnqueueEnabledJobs(String trigger, boolean force) {
        log.info("SysJobMapper proxy class = {}", jobMapper.getClass());

        if (!force && !bootstrapped.compareAndSet(false, true)) {
            log.info("[JOB_BOOTSTRAP_SKIP] trigger={} reason=already_bootstrapped", trigger);
            return;
        }

        List<SysJob> jobs = jobMapper.selectJobAll();
        if (jobs == null || jobs.isEmpty()) {
            log.info("[JOB_BOOTSTRAP_EMPTY] trigger={}", trigger);
            return;
        }

        log.info("[JOB_BOOTSTRAP_START] trigger={} total={}", trigger, jobs.size());

        for (SysJob job : jobs) {
            if (!Objects.equals("0", job.getStatus())) {
                continue;
            }
            enqueueNextExecution(job);
        }

        log.info("[JOB_BOOTSTRAP_DONE] trigger={}", trigger);
    }

    /* ========================= 查询接口 ========================= */

    @Override
    public List<SysJob> selectJobList(SysJob sysJob) {
        return jobMapper.selectJobList(sysJob);
    }

    @Override
    public SysJob selectJobById(Long jobId) {
        return jobMapper.selectJobById(jobId);
    }

    @Override
    public int deleteJob(SysJob job) {
        if (job == null || job.getJobId() == null) {
            return 0;
        }

        return jobMapper.deleteJobByIds(new Long[]{job.getJobId()});
    }


    @Override
    public List<SysJob> selectJobAll() {
        return jobMapper.selectJobAll();
    }

    /* ========================= 写接口 ========================= */

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

    /**
     * 启停任务
     *
     * <p>注意：Mapper 中没有 changeStatus，
     * 实际就是一次 updateJob
     */
    @Override
    public int changeStatus(SysJob job) {
        int rows = jobMapper.updateJob(job);

        // 仅在启用时补充调度
        if (rows > 0 && Objects.equals("0", job.getStatus())) {
            enqueueNextExecution(job);
        }

        return rows;
    }

    /* ========================= 工具方法 ========================= */

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
                log.warn(
                        "[JOB_SCHEDULE_SKIP] jobId={} jobName={} reason=no_next_time cron={}",
                        job.getJobId(),
                        job.getJobName(),
                        cron
                );
                return;
            }

            long nextAtMillis = nextTime.toInstant().toEpochMilli();
            taskDistributor.scheduleJob(job, nextAtMillis);

            log.info(
                    "[JOB_SCHEDULE] jobId={} jobName={} nextAtMillis={}",
                    job.getJobId(),
                    job.getJobName(),
                    nextAtMillis
            );
        } catch (Exception e) {
            log.warn(
                    "[JOB_SCHEDULE_ERR] jobId={} cron={}",
                    job.getJobId(),
                    cron,
                    e
            );
        }
    }
}
