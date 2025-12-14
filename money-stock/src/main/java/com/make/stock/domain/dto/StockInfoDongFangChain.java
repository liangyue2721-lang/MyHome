package com.make.stock.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * 东方财富内存中对象 stock_info_dongFang
 *
 * @author 贰柒
 * @date 2025-02-19
 */
@Data
@Jacksonized
@Builder
public class StockInfoDongFangChain {
    private static final long serialVersionUID = 1L;

    /**
     * 股票代码
     */
    @JsonProperty("f12")
    private String stockCode;

    /**
     * 股票名称
     */
    @JsonProperty("f14")
    private String companyName;

    /**
     * 最新价
     */
    @JsonProperty("f2")
    private Double price;

    /**
     * 涨跌幅（百分比）
     */
    @JsonProperty("f3")
    private Double netChangePercentage;

    /**
     * 涨跌额
     */
    @JsonProperty("f4")
    private Double netChange;

    /**
     * 成交量（手）
     */
    @JsonProperty("f5")
    private Long volume;

    /**
     * 成交额（元）
     */
    @JsonProperty("f6")
    private Double turnover;

    /**
     * 振幅（百分比）
     */
    @JsonProperty("f7")
    private Double amplitude;

    /**
     * 换手率（百分比）
     */
    @JsonProperty("f8")
    private Double turnoverRate;

    /**
     * 市盈率
     */
    @JsonProperty("f9")
    private Double peRatio;

    /**
     * 市净率
     */
    @JsonProperty("f10")
    private Double pbRatio;

    /**
     * 最高价
     */
    @JsonProperty("f15")
    private Double highPrice;

    /**
     * 最低价
     */
    @JsonProperty("f16")
    private Double lowPrice;

    /**
     * 开盘价
     */
    @JsonProperty("f17")
    private Double openPrice;

    /**
     * 昨日收盘价
     */
    @JsonProperty("f18")
    private Double prevClose;

    /**
     * 流通市值（元）
     */
    @JsonProperty("f23")
    private Double circulatingMarketValue;

    /**
     * 主力资金净流入（元）
     */
    @JsonProperty("f152")
    private Double mainFundsInflow;

    /**
     * 市场类型（1: 沪市，0: 深市）
     */
    @JsonProperty("f13")
    private Integer marketType;

    /**
     * 量比（成交量比率）
     * 计算公式：$量比=\frac{当前成交量}{过去5日平均成交量}$
     */
    @JsonProperty("f20")
    private Double volumeRatio;

    /**
     * 委比（委托买卖比例）
     * 计算公式：$委比=\frac{委买量-委卖量}{委买量+委卖量}×100\%$
     */
    @JsonProperty("f21")
    private Double commissionRatio;

    /**
     * 委差（委托买卖差额）
     * 计算公式：$委差=委买总量-委卖总量$
     */
    @JsonProperty("f22")
    private Long commissionDifference;

    /**
     * 每股收益（EPS）
     * 计算公式：$EPS=\frac{净利润}{总股本}$
     */
    @JsonProperty("f62")
    private Double eps;

    /**
     * 主力净流入（单位：元）
     * 包含超大单和大单资金净流入
     */
    @JsonProperty("f128")
    private Double mainNetInflow;

    /**
     * 流通股本（单位：股）
     */
    @JsonProperty("f136")
    private Long circulatingShares;

    /**
     * 总股本（单位：股）
     */
    @JsonProperty("f115")
    private Double totalShares;

    /**
     * 量价趋势指标
     * 0: 无趋势 1: 量价齐升 2: 量升价跌
     */
    @JsonProperty("f24")
    private Integer volumePriceTrend;

    /**
     * 股息率（百分比）
     * 计算公式：$股息率=\frac{年度分红总额}{当前股价}×100\%$
     */
    @JsonProperty("f25")
    private Double dividendYield;

    @JsonProperty("f60")
    private Double roe; // 净资产收益率
    @JsonProperty("f61")
    private Double grossMargin; // 毛利率

    @JsonProperty("f100")
    private Double institutionalFlow; // 机构资金净流入

    @JsonProperty("f101")
    private Double retailFlow; // 散户资金流向

    /**
     * 安全解析工厂方法（完整版）
     */
    public static StockInfoDongFangChain parse(JsonNode node) {
        return StockInfoDongFangChain.builder()
                .stockCode(safeGetText(node, "f12"))
                .companyName(safeGetText(node, "f14"))
                .price(parseDouble(node, "f2"))
                .netChangePercentage(parseDouble(node, "f3"))
                .netChange(parseDouble(node, "f4"))
                .volume(safeGetLong(node, "f5"))
                .turnover(parseDouble(node, "f6"))
                .amplitude(parseDouble(node, "f7"))
                .turnoverRate(parseDouble(node, "f8"))
                .peRatio(parseDouble(node, "f9"))
                .pbRatio(parseDouble(node, "f10"))
                .highPrice(parseDouble(node, "f15"))
                .lowPrice(parseDouble(node, "f16"))
                .openPrice(parseDouble(node, "f17"))
                .prevClose(parseDouble(node, "f18"))
                .circulatingMarketValue(parseDouble(node, "f23"))
                .mainFundsInflow(parseDouble(node, "f152"))
                .marketType(safeGetInt(node, "f13"))
                .volumeRatio(parseDouble(node, "f20"))
                .commissionRatio(parseDouble(node, "f21"))
                .commissionDifference(safeGetLong(node, "f22"))
                .eps(parseDouble(node, "f62"))
                .mainNetInflow(parseDouble(node, "f128"))
                .circulatingShares(safeGetLong(node, "f136"))
                .totalShares(parseDouble(node, "f115"))
                .volumePriceTrend(safeGetInt(node, "f24"))
                .dividendYield(parseDouble(node, "f25"))
                .roe(parseDouble(node, "f60"))
                .grossMargin(parseDouble(node, "f61"))
                .institutionalFlow(parseDouble(node, "f100"))
                .retailFlow(parseDouble(node, "f101"))
//                .bidLevels(parseBidLevels(node))
//                .askLevels(parseAskLevels(node))
                .build();
    }

    /**
     * 安全解析工厂方法（完整版）
     */
    public static StockInfoDongFangChain parse2(JsonNode node) {
        return StockInfoDongFangChain.builder()
                .stockCode(safeGetText(node, "f57"))
                .companyName(safeGetText(node, "f58"))
                .price(parseDouble(node, "f43") / 100)
                .prevClose(parseDouble(node, "f60") / 100)
                .prevClose(parseDouble(node, "f60") / 100)
                .prevClose(parseDouble(node, "f60") / 100)
                .highPrice(parseDouble(node, "f15")/ 100)
                .lowPrice(parseDouble(node, "f16")/ 100)
                .build();
    }

    // 解析工具方法
    private static String safeGetText(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    private static Long safeGetLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0L;
    }

    private static Integer safeGetInt(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asInt() : 0;
    }

    private static Double parseDouble(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asDouble() : 0.0;
    }
}
