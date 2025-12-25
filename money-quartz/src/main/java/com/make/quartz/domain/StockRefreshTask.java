package com.make.quartz.domain;

import java.io.Serializable;

/**
 * 股票刷新任务子任务对象
 */
public class StockRefreshTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID (UUID)
     */
    private String taskId;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 任务类型 (e.g. REFRESH_PRICE)
     */
    private String taskType;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 追踪ID (继承自父任务)
     */
    private String traceId;

    public StockRefreshTask() {
    }

    public StockRefreshTask(String taskId, String stockCode, String taskType, long createTime, String traceId) {
        this.taskId = taskId;
        this.stockCode = stockCode;
        this.taskType = taskType;
        this.createTime = createTime;
        this.traceId = traceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "StockRefreshTask{" +
                "taskId='" + taskId + '\'' +
                ", stockCode='" + stockCode + '\'' +
                ", taskType='" + taskType + '\'' +
                ", createTime=" + createTime +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
