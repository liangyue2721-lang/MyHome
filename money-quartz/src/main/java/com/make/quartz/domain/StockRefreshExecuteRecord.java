package com.make.quartz.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 刷新任务执行记录对象 stock_refresh_execute_record
 *
 * @author erqi
 * @date 2025-12-26
 */
public class StockRefreshExecuteRecord extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 股票代码 */
    @Excel(name = "股票代码")
    private String stockCode;

    /** 股票名称 */
    @Excel(name = "股票名称")
    private String stockName;

    /** 任务状态（SUCCESS / FAILED） */
    @Excel(name = "任务状态", readConverterExp = "S=UCCESS,F=AILED")
    private String status;

    /** 执行结果（如 INVALID_URL） */
    @Excel(name = "执行结果", readConverterExp = "I=NVALID_URL")
    private String executeResult;

    /** 执行节点IP */
    @Excel(name = "执行节点IP")
    private String nodeIp;

    /** 任务执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任务执行时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date executeTime;

    /** Trace ID */
    @Excel(name = "Trace ID")
    private String traceId;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
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

    public void setStockName(String stockName)
    {
        this.stockName = stockName;
    }

    public String getStockName()
    {
        return stockName;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setExecuteResult(String executeResult)
    {
        this.executeResult = executeResult;
    }

    public String getExecuteResult()
    {
        return executeResult;
    }

    public void setNodeIp(String nodeIp)
    {
        this.nodeIp = nodeIp;
    }

    public String getNodeIp()
    {
        return nodeIp;
    }

    public void setExecuteTime(Date executeTime)
    {
        this.executeTime = executeTime;
    }

    public Date getExecuteTime()
    {
        return executeTime;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getTraceId()
    {
        return traceId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("stockCode", getStockCode())
            .append("stockName", getStockName())
            .append("status", getStatus())
            .append("executeResult", getExecuteResult())
            .append("nodeIp", getNodeIp())
            .append("executeTime", getExecuteTime())
            .append("traceId", getTraceId())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
