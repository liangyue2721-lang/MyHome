package com.make.quartz.domain;

import java.io.Serializable;

/**
 * 股票任务状态对象 (用于监控)
 */
public class StockTaskStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_OCCUPIED = "OCCUPIED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private String stockCode;
    private String status;
    private String occupiedByNode; // Node IP
    private long occupiedTime;
    private long lastUpdateTime;
    private String traceId;
    private String lastResult; // Optional: error message or success details

    public StockTaskStatus() {
    }

    public StockTaskStatus(String stockCode, String status, String occupiedByNode, long occupiedTime, String traceId) {
        this.stockCode = stockCode;
        this.status = status;
        this.occupiedByNode = occupiedByNode;
        this.occupiedTime = occupiedTime;
        this.traceId = traceId;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOccupiedByNode() {
        return occupiedByNode;
    }

    public void setOccupiedByNode(String occupiedByNode) {
        this.occupiedByNode = occupiedByNode;
    }

    public long getOccupiedTime() {
        return occupiedTime;
    }

    public void setOccupiedTime(long occupiedTime) {
        this.occupiedTime = occupiedTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLastResult() {
        return lastResult;
    }

    public void setLastResult(String lastResult) {
        this.lastResult = lastResult;
    }
}
