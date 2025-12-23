package com.make.quartz.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 实时任务（待执行 / 执行中）对象 sys_job_runtime
 *
 * @author erqi
 * @date 2025-12-23
 */
public class SysJobRuntime extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 业务任务ID（关联 sys_job） */
    @Excel(name = "业务任务ID", readConverterExp = "关=联,s=ys_job")
    private Long jobId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String jobName;

    /** 任务分组 */
    @Excel(name = "任务分组")
    private String jobGroup;

    /** 本次执行ID（全局唯一，用于关联日志） */
    @Excel(name = "本次执行ID", readConverterExp = "全=局唯一，用于关联日志")
    private String executionId;

    /** 任务状态：WAITING / RUNNING / RETRYING */
    @Excel(name = "任务状态：WAITING / RUNNING / RETRYING")
    private String status;

    /** 当前执行节点ID */
    @Excel(name = "当前执行节点ID")
    private String nodeId;

    /** 计划执行时间（调度时间） */
    @Excel(name = "计划执行时间", readConverterExp = "调=度时间")
    private Date scheduledTime;

    /** 进入队列时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "进入队列时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date enqueueTime;

    /** 实际开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "实际开始执行时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 已重试次数 */
    @Excel(name = "已重试次数")
    private Long retryCount;

    /** 最大允许重试次数 */
    @Excel(name = "最大允许重试次数")
    private Long maxRetry;

    /** 任务执行参数（JSON） */
    @Excel(name = "任务执行参数", readConverterExp = "J=SON")
    private String payload;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setJobId(Long jobId)
    {
        this.jobId = jobId;
    }

    public Long getJobId()
    {
        return jobId;
    }

    public void setJobName(String jobName)
    {
        this.jobName = jobName;
    }

    public String getJobName()
    {
        return jobName;
    }

    public void setJobGroup(String jobGroup)
    {
        this.jobGroup = jobGroup;
    }

    public String getJobGroup()
    {
        return jobGroup;
    }

    public void setExecutionId(String executionId)
    {
        this.executionId = executionId;
    }

    public String getExecutionId()
    {
        return executionId;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public void setScheduledTime(Date scheduledTime)
    {
        this.scheduledTime = scheduledTime;
    }

    public Date getScheduledTime()
    {
        return scheduledTime;
    }

    public void setEnqueueTime(Date enqueueTime)
    {
        this.enqueueTime = enqueueTime;
    }

    public Date getEnqueueTime()
    {
        return enqueueTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setRetryCount(Long retryCount)
    {
        this.retryCount = retryCount;
    }

    public Long getRetryCount()
    {
        return retryCount;
    }

    public void setMaxRetry(Long maxRetry)
    {
        this.maxRetry = maxRetry;
    }

    public Long getMaxRetry()
    {
        return maxRetry;
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }

    public String getPayload()
    {
        return payload;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("jobId", getJobId())
            .append("jobName", getJobName())
            .append("jobGroup", getJobGroup())
            .append("executionId", getExecutionId())
            .append("status", getStatus())
            .append("nodeId", getNodeId())
            .append("scheduledTime", getScheduledTime())
            .append("enqueueTime", getEnqueueTime())
            .append("startTime", getStartTime())
            .append("retryCount", getRetryCount())
            .append("maxRetry", getMaxRetry())
            .append("payload", getPayload())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
