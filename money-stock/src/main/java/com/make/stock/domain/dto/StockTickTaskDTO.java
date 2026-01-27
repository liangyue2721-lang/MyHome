package com.make.stock.domain.dto;

import java.io.Serializable;

/**
 * 股票逐笔成交任务 DTO
 */
public class StockTickTaskDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 追踪ID */
    private String traceId;

    /** 股票代码 */
    private String stockCode;

    /** 市场代码 (1:SH, 0:SZ) */
    private String market;

    public StockTickTaskDTO() {}

    public StockTickTaskDTO(String traceId, String stockCode, String market) {
        this.traceId = traceId;
        this.stockCode = stockCode;
        this.market = market;
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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    @Override
    public String toString() {
        return "StockTickTaskDTO{" +
                "traceId='" + traceId + '\'' +
                ", stockCode='" + stockCode + '\'' +
                ", market='" + market + '\'' +
                '}';
    }
}
