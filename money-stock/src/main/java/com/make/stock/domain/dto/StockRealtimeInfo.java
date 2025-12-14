package com.make.stock.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockRealtimeInfo {

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String companyName;

    /**
     * 最新价格
     */
    private Double price;

    /**
     * 昨日收盘价
     */
    private Double prevClose;

    /**
     * 今日开盘价
     */
    private Double openPrice;

    /**
     * 今日最高价
     */
    private Double highPrice;

    /**
     * 今日最低价
     */
    private Double lowPrice;

    /**
     * 成交量（手）
     */
    private Long volume;

    /**
     * 成交额（元）
     */
    private Double turnover;

    /**
     * 量比（f52）
     */
    private Double volumeRatio;

    /**
     * 委比（f20）
     */
    private Double commissionRatio;

    /**
     * 主力资金净流入（f152）
     */
    private Double mainFundsInflow;

    // =========== 以下为可能需要但脚本默认设为 null 的字段 ===========

    /**
     * 市盈率（如返回可填）
     */
    private Double peRatio;

    /**
     * 市净率
     */
    private Double pbRatio;

    /**
     * 换手率
     */
    private Double turnoverRate;

    /**
     * 振幅
     */
    private Double amplitude;

    /**
     * 每股收益（EPS）
     */
    private Double eps;

    /**
     * 主力净流入（大单+中单等）
     */
    private Double mainNetInflow;

    /**
     * 流通股本（股）
     */
    private Long circulatingShares;

    /**
     * 总股本（股）
     */
    private Double totalShares;

    /**
     * 量价趋势
     */
    private Integer volumePriceTrend;

    /**
     * 股息率
     */
    private Double dividendYield;

    /**
     * 净资产收益率（ROE）
     */
    private Double roe;

    /**
     * 毛利率
     */
    private Double grossMargin;

    /**
     * 机构资金流向
     */
    private Double institutionalFlow;

    /**
     * 散户资金流向
     */
    private Double retailFlow;
}
