package com.make.quartz.service.impl;

import java.util.List;
import javax.annotation.PostConstruct;

import com.make.common.utils.spring.SpringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.mapper.SysJobMapper;
import com.make.quartz.util.CronUtils;
import com.make.quartz.util.ScheduleUtils;
import com.make.quartz.service.ISysJobService;
import com.make.quartz.util.SchedulerManager;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.make.common.constant.ScheduleConstants;
import com.make.common.exception.job.TaskException;

/**
 * 定时任务调度信息 服务层
 *
 * @author ruoyi
 */
@Service
public class SysJobServiceImpl implements ISysJobService {
    private static final Logger log = LoggerFactory.getLogger(SysJobServiceImpl.class);
    
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysJobMapper jobMapper;

    /**
     * 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        log.info("🔄 开始初始化定时任务调度器...");
        
        if (scheduler == null) {
            log.error("❌ 调度器未初始化");
            throw new SchedulerException("调度器未正确初始化");
        }
        
        try {
            scheduler.clear();
            log.info("✅ 调度器已清空");
        } catch (SchedulerException e) {
            log.error("❌ 清空调度器时发生异常", e);
            throw e;
        }
        
        List<SysJob> jobList;
        try {
            jobList = jobMapper.selectJobAll();
            log.info("📋 从数据库加载到 {} 个任务", jobList.size());
        } catch (Exception e) {
            log.error("❌ 从数据库加载任务列表时发生异常", e);
            throw new TaskException("从数据库加载任务列表失败", e);
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (SysJob job : jobList) {
            try {
                log.info("🔧 正在创建任务: ID={}, 名称={}, 组名={}", job.getJobId(), job.getJobName(), job.getJobGroup());
                ScheduleUtils.createScheduleJob(scheduler, job);
                successCount++;
                log.info("✅ 任务创建成功: ID={}, 名称={}", job.getJobId(), job.getJobName());
            } catch (Exception e) {
                failCount++;
                log.error("❌ 创建任务失败: ID={}, 名称={}, 错误信息: {}", job.getJobId(), job.getJobName(), e.getMessage(), e);
                // 继续处理其他任务，不因单个任务失败而中断整个初始化过程
            }
        }
        
        log.info("🏁 定时任务初始化完成: 成功={}个, 失败={}个, 总计={}个", successCount, failCount, jobList.size());
    }

    /**
     * 获取quartz调度器的计划任务列表
     *
     * @param job 调度信息
     * @return
     */
    @Override
    public List<SysJob> selectJobList(SysJob job) {
        return jobMapper.selectJobList(job);
    }

    /**
     * 通过调度任务ID查询调度信息
     *
     * @param jobId 调度任务ID
     * @return 调度任务对象信息
     */
    @Override
    public SysJob selectJobById(Long jobId) {
        return jobMapper.selectJobById(jobId);
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pauseJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resumeJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 删除任务后，删除quartz调度器中对应的任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = jobMapper.deleteJobById(jobId);
        if (rows > 0) {
            // 使用增强的删除方法确保数据一致性
            ScheduleUtils.deleteScheduleJob(scheduler, jobId, jobGroup);
        }
        return rows;
    }

    /**
     * 批量删除调度信息
     *
     * @param jobIds 调度任务ID数组
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException {
        for (Long jobId : jobIds) {
            SysJob job = jobMapper.selectJobById(jobId);
            // 使用增强的删除方法确保数据一致性
            ScheduleUtils.deleteScheduleJob(scheduler, jobId, job.getJobGroup());
        }
        jobMapper.deleteJobByIds(jobIds);
    }

    /**
     * 任务调度状态修改
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(SysJob job) throws SchedulerException {
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            Long jobId = job.getJobId();
            String jobGroup = job.getJobGroup();
            if (ScheduleConstants.Status.NORMAL.getValue().equals(job.getStatus())) {
                scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
            } else if (ScheduleConstants.Status.PAUSE.getValue().equals(job.getStatus())) {
                scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
            }
        }
        return rows;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(SysJob job) throws SchedulerException {
        boolean result = false;
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        SysJob properties = selectJobById(job.getJobId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            result = true;
            scheduler.triggerJob(jobKey, dataMap);
        }
        return result;
    }

    /**
     * 新增任务
     *
     * @param job 调度信息 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertJob(SysJob job) throws SchedulerException, TaskException {
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = jobMapper.insertJob(job);
        if (rows > 0) {
            // 将isMasterNode属性保存到Redis中
            saveJobIsMasterNodeToRedis(job);
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return rows;
    }

    /**
     * 更新任务的时间表达式
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateJob(SysJob job) throws SchedulerException, TaskException {
        SysJob properties = selectJobById(job.getJobId());
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            // 将isMasterNode属性保存到Redis中
            saveJobIsMasterNodeToRedis(job);
            updateSchedulerJob(job, properties.getJobGroup());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException {
        Long jobId = job.getJobId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 将任务的isMasterNode属性保存到Redis中
     * @param job 任务信息
     */
    private void saveJobIsMasterNodeToRedis(SysJob job) {
        try {
            SchedulerManager schedulerManager = SpringUtils.getBean(SchedulerManager.class);
            if (schedulerManager != null && job.getJobId() != null) {
                String isMasterNode = job.getIsMasterNode();
                if (isMasterNode == null) {
                    isMasterNode = "0"; // 默认值
                }
                schedulerManager.setJobIsMasterNode(job.getJobId(), isMasterNode);
            }
        } catch (Exception e) {
            // 记录日志但不中断主流程
            log.warn("保存任务的isMasterNode属性到Redis失败，任务ID: {}", job.getJobId(), e);
        }
    }

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }
}
