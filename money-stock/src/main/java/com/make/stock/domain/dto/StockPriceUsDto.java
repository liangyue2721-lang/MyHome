package com.make.stock.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;


import java.math.BigDecimal;

/**
 * 美股阶段行情信息对象 stock_price_us
 *
 * @author erqi
 * @date 2025-10-26
 */
public class StockPriceUsDto {

    /**
     * 股票代码
     */
    @JsonProperty("f57")
    private String code;

    /**
     * 股票名称
     */
    @JsonProperty("f58")
    private String name;

    /**
     * 最新价（单位：元）
     */
    @JsonProperty("f43")
    private BigDecimal priceNow;

    /**
     * 当日最高价
     */
    @JsonProperty("f44")
    private BigDecimal priceHighDay;

    /**
     * 当日最低价
     */
    @JsonProperty("f45")
    private BigDecimal priceLowDay;

    /**
     * 今日开盘价
     */
    @JsonProperty("f46")
    private BigDecimal priceOpenDay;

    /**
     * 昨日收盘价
     */
    @JsonProperty("f12")
    private BigDecimal priceCloseYesterday;

    // -------------------- 链式Setter方法 --------------------

    public StockPriceUsDto setCode(String code) {
        this.code = code;
        return this;
    }

    public StockPriceUsDto setName(String name) {
        this.name = name;
        return this;
    }

    public StockPriceUsDto setPriceNow(BigDecimal priceNow) {
        this.priceNow = priceNow;
        return this;
    }

    public StockPriceUsDto setPriceHighDay(BigDecimal priceHighDay) {
        this.priceHighDay = priceHighDay;
        return this;
    }

    public StockPriceUsDto setPriceLowDay(BigDecimal priceLowDay) {
        this.priceLowDay = priceLowDay;
        return this;
    }

    public StockPriceUsDto setPriceOpenDay(BigDecimal priceOpenDay) {
        this.priceOpenDay = priceOpenDay;
        return this;
    }

    public StockPriceUsDto setPriceCloseYesterday(BigDecimal priceCloseYesterday) {
        this.priceCloseYesterday = priceCloseYesterday;
        return this;
    }


    // -------------------- 标准Setter/Getter方法 --------------------



    public String getCode() {
        return code;
    }


    public String getName() {
        return name;
    }


    public BigDecimal getPriceNow() {
        return priceNow;
    }


    public BigDecimal getPriceHighDay() {
        return priceHighDay;
    }


    public BigDecimal getPriceLowDay() {
        return priceLowDay;
    }


    public BigDecimal getPriceOpenDay() {
        return priceOpenDay;
    }


    public BigDecimal getPriceCloseYesterday() {
        return priceCloseYesterday;
    }

    /**
     * f57	"AAPL"	股票代码
     * f58	"苹果"	股票名称
     * f43	262240	最新价（单位可能需换算）
     * f44	264375	当日最高价
     * f45	255630	当日最低价
     * f46	255885	今日开盘价
     * f60	252290	昨日收盘价
     * 安全解析工厂方法（支持链式存储）
     */
    public static StockPriceUsDto parse(JsonNode node) {
        return new StockPriceUsDto()
                .setCode(safeGetText(node, "f57"))
                .setName(safeGetText(node, "f58"))
                .setPriceNow(parseBigDecimal(node, "f43").divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP))
                .setPriceHighDay(parseBigDecimal(node, "f44").divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP))
                .setPriceLowDay(parseBigDecimal(node, "f45").divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP))
                .setPriceOpenDay(parseBigDecimal(node, "f46").divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP))
                .setPriceCloseYesterday(parseBigDecimal(node, "f60").divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP));
    }

    /**
     * 安全获取文本值
     */
    private static String safeGetText(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    /**
     * 安全解析BigDecimal值
     */
    private static BigDecimal parseBigDecimal(JsonNode node, String field) {
        return node.has(field) ? BigDecimal.valueOf(node.get(field).asDouble()) : BigDecimal.ZERO;
    }


}
