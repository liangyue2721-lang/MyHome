package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 主力资金流向对象 stock_fund_flow
 *
 * @author erqi
 * @date 2026-05-07
 */
@Data
public class StockFundFlow extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 交易日期 (按天分区或查询过滤使用)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "交易日期 (按天分区或查询过滤使用)", width = 30, dateFormat = "yyyy-MM-dd")
    private Date tradeDate;

    /**
     * 股票代码
     */
    @Excel(name = "股票代码")
    private String stockCode;

    /**
     * 股票名称
     */
    @Excel(name = "股票名称")
    private String stockName;

    /**
     * 市场类型: 0-深交所, 1-上交所
     */
    @Excel(name = "市场类型: 0-深交所, 1-上交所")
    private Long marketType;

    /**
     * 最新价 (元)
     */
    @Excel(name = "最新价 (元)")
    private BigDecimal latestPrice;

    /**
     * 涨跌幅 (%)
     */
    @Excel(name = "涨跌幅 (%)")
    private BigDecimal changePercent;

    /**
     * 当天成交总额 (元)
     */
    @Excel(name = "当天成交总额 (元)")
    private BigDecimal totalAmount;

    /**
     * 主力净流入净额 (元)
     */
    @Excel(name = "主力净流入净额 (元)")
    private BigDecimal mainNetInflow;

    /**
     * 主力净流入占比 (%)
     */
    @Excel(name = "主力净流入占比 (%)")
    private BigDecimal mainInflowRatio;

    /**
     * 超大单净流入额 (元)
     */
    @Excel(name = "超大单净流入额 (元)")
    private BigDecimal superLargeInflow;

    /**
     * 超大单净流入占比 (%)
     */
    @Excel(name = "超大单净流入占比 (%)")
    private BigDecimal superLargeRatio;

    /**
     * 大单净流入额 (元)
     */
    @Excel(name = "大单净流入额 (元)")
    private BigDecimal largeInflow;

    /**
     * 大单净流入占比 (%)
     */
    @Excel(name = "大单净流入占比 (%)")
    private BigDecimal largeRatio;

    /**
     * 中单净流入额 (元)
     */
    @Excel(name = "中单净流入额 (元)")
    private BigDecimal mediumInflow;

    /**
     * 中单净流入占比 (%)
     */
    @Excel(name = "中单净流入占比 (%)")
    private BigDecimal mediumRatio;

    /**
     * 小单净流入额 (元)
     */
    @Excel(name = "小单净流入额 (元)")
    private BigDecimal smallInflow;

    /**
     * 小单净流入占比 (%)
     */
    @Excel(name = "小单净流入占比 (%)")
    private BigDecimal smallRatio;

    /**
     * 源数据更新时间戳 (Unix秒)
     */
    @Excel(name = "源数据更新时间戳 (Unix秒)")
    private Long dataTimestamp;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    /**
     * 超大单绝对流入 (元)
     */
    @Excel(name = "超大单绝对流入 (元)")
    private BigDecimal superLargeAbsInflow;

    /**
     * 超大单流出 (元)
     */
    @Excel(name = "超大单流出 (元)")
    private BigDecimal superLargeOutflow;

    /**
     * 大单绝对流入 (元)
     */
    @Excel(name = "大单绝对流入 (元)")
    private BigDecimal largeAbsInflow;

    /**
     * 大单流出 (元)
     */
    @Excel(name = "大单流出 (元)")
    private BigDecimal largeAbsOutflow;

    /**
     * 中单绝对流入 (元)
     */
    @Excel(name = "中单绝对流入 (元)")
    private BigDecimal mediumAbsInflow;

    /**
     * 中单流出 (元)
     */
    @Excel(name = "中单流出 (元)")
    private BigDecimal mediumAbsOutflow;

    /**
     * 小单绝对流入 (元)
     */
    @Excel(name = "小单绝对流入 (元)")
    private BigDecimal smallAbsInflow;

    /**
     * 小单流出 (元)
     */
    @Excel(name = "小单流出 (元)")
    private BigDecimal smallAbsOutflow;

    /**
     * 排行天数 (前端查询参数)
     */
    private Integer rankDays;


    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("tradeDate", getTradeDate())
                .append("stockCode", getStockCode())
                .append("stockName", getStockName())
                .append("marketType", getMarketType())
                .append("latestPrice", getLatestPrice())
                .append("changePercent", getChangePercent())
                .append("totalAmount", getTotalAmount())
                .append("mainNetInflow", getMainNetInflow())
                .append("mainInflowRatio", getMainInflowRatio())
                .append("superLargeInflow", getSuperLargeInflow())
                .append("superLargeRatio", getSuperLargeRatio())
                .append("largeInflow", getLargeInflow())
                .append("largeRatio", getLargeRatio())
                .append("mediumInflow", getMediumInflow())
                .append("mediumRatio", getMediumRatio())
                .append("smallInflow", getSmallInflow())
                .append("smallRatio", getSmallRatio())
                .append("dataTimestamp", getDataTimestamp())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .append("superLargeAbsInflow", getSuperLargeAbsInflow())
                .append("superLargeOutflow", getSuperLargeOutflow())
                .append("largeAbsInflow", getLargeAbsInflow())
                .append("largeAbsOutflow", getLargeAbsOutflow())
                .append("mediumAbsInflow", getMediumAbsInflow())
                .append("mediumAbsOutflow", getMediumAbsOutflow())
                .append("smallAbsInflow", getSmallAbsInflow())
                .append("smallAbsOutflow", getSmallAbsOutflow())
                .toString();
    }
}
