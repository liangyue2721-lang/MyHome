package com.make.quartz.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 任务执行历史记录对象 sys_job_execution_log
 *
 * @author erqi
 * @date 2025-12-22
 */
public class SysJobExecutionLog extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 业务任务ID */
    @Excel(name = "业务任务ID")
    private Long jobId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String jobName;

    /** 任务分组 */
    @Excel(name = "任务分组")
    private String jobGroup;

    /** 执行ID（与 runtime 表一致） */
    @Excel(name = "执行ID", readConverterExp = "r=untime,表=一致")
    private String executionId;

    /** 执行结果：SUCCESS / FAILED / TIMEOUT / CANCELED */
    @Excel(name = "执行结果：SUCCESS / FAILED / TIMEOUT / CANCELED")
    private String status;

    /** 执行节点ID */
    @Excel(name = "执行节点ID")
    private String nodeId;

    /** 计划执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划执行时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date scheduledTime;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始执行时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 执行结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "执行结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 执行耗时（毫秒） */
    @Excel(name = "执行耗时", readConverterExp = "毫=秒")
    private Long durationMs;

    /** 本次执行的重试次数 */
    @Excel(name = "本次执行的重试次数")
    private Long retryCount;

    /** 失败原因摘要 */
    @Excel(name = "失败原因摘要")
    private String errorMessage;

    /** 失败堆栈信息 */
    @Excel(name = "失败堆栈信息")
    private String errorStack;

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

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setRetryCount(Long retryCount)
    {
        this.retryCount = retryCount;
    }

    public Long getRetryCount()
    {
        return retryCount;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorStack(String errorStack)
    {
        this.errorStack = errorStack;
    }

    public String getErrorStack()
    {
        return errorStack;
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
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("durationMs", getDurationMs())
            .append("retryCount", getRetryCount())
            .append("errorMessage", getErrorMessage())
            .append("errorStack", getErrorStack())
            .append("payload", getPayload())
            .append("createTime", getCreateTime())
            .toString();
    }
}
