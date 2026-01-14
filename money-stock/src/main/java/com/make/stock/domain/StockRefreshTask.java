package com.make.stock.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

/**
 * 股票刷新任务对象
 * 用于 Redis 队列传输
 */
public class StockRefreshTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务ID (executionId) - differs from traceId */
    private String taskId;

    /** 批次追踪ID */
    private String traceId;

    /** 股票代码 */
    private String stockCode;

    /** 任务类型 (REFRESH_PRICE, KLINE, etc) */
    private String taskType;

    /** 优先级 (HIGH/NORMAL) */
    private String priority;

    /** 重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private Long createTime;

    public StockRefreshTask() {}

    public StockRefreshTask(String traceId, String stockCode, String taskType) {
        this.traceId = traceId;
        this.stockCode = stockCode;
        this.taskType = taskType;
        this.retryCount = 0;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("taskId", getTaskId())
                .append("traceId", getTraceId())
                .append("stockCode", getStockCode())
                .append("taskType", getTaskType())
                .append("priority", getPriority())
                .append("retryCount", getRetryCount())
                .append("createTime", getCreateTime())
                .toString();
    }
}
