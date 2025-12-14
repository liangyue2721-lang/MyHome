package com.make.stock.domain;

import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 股票K线数据任务对象 stock_kline_task
 *
 * @author erqi
 * @date 2025-11-03
 */
public class StockKlineTask extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 股票代码，例如 600519 */
    @Excel(name = "股票代码，例如 600519")
    private String stockCode;

    /** 市场标识，如 SH、SZ */
    @Excel(name = "市场标识，如 SH、SZ")
    private String market;

    /** 执行状态 */
    @Excel(name = "执行状态")
    private Long taskStatus;

    /** 计划执行时间 */
    @Excel(name = "计划执行时间")
    private Date executeTime;

    /** 节点ID */
    @Excel(name = "节点ID")
    private Long nodeId;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setStockCode(String stockCode)
    {
        this.stockCode = stockCode;
    }

    public String getStockCode()
    {
        return stockCode;
    }

    public void setMarket(String market)
    {
        this.market = market;
    }

    public String getMarket()
    {
        return market;
    }

    public void setTaskStatus(Long taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    public Long getTaskStatus()
    {
        return taskStatus;
    }

    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("stockCode", getStockCode())
            .append("market", getMarket())
            .append("taskStatus", getTaskStatus())
            .append("executeTime", getExecuteTime())
            .append("nodeId", getNodeId())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}