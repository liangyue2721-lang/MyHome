package com.make.stock.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EtfRealtimeInfo {

    /**
     * ETF 代码
     */
    private String stockCode;

    /**
     * ETF 名称
     */
    private String companyName;

    /**
     * 最新价格
     */
    private Double price;

    /**
     * 昨日收盘
     */
    private Double prevClose;

    /**
     * 今日开盘
     */
    private Double openPrice;

    /**
     * 今日最高
     */
    private Double highPrice;

    /**
     * 今日最低
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
     * 量比
     */
    private Double volumeRatio;

    /**
     * 委比
     */
    private Double commissionRatio;

    /**
     * 主力资金净流入
     */
    private Double mainFundsInflow;
}
