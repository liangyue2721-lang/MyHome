package com.make.stock.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 美股阶段行情信息对象 stock_price_us
 *
 * @author erqi
 * @date 2025-10-26
 */
public class StockPriceUs extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 股票代码
     */
    @Excel(name = "股票代码")
    private String code;

    /**
     * 股票名称
     */
    @Excel(name = "股票名称")
    private String name;

    /**
     * 最新价（单位：元）
     */
    @Excel(name = "最新价", readConverterExp = "单=位：元")
    private BigDecimal priceNow;

    /**
     * 当日最高价
     */
    @Excel(name = "当日最高价")
    private BigDecimal priceHighDay;

    /**
     * 当日最低价
     */
    @Excel(name = "当日最低价")
    private BigDecimal priceLowDay;

    /**
     * 今日开盘价
     */
    @Excel(name = "今日开盘价")
    private BigDecimal priceOpenDay;

    /**
     * 昨日收盘价
     */
    @Excel(name = "昨日收盘价")
    private BigDecimal priceCloseYesterday;

    /**
     * 周内最低价
     */
    @Excel(name = "周内最低价")
    private BigDecimal priceLowWeek;

    /**
     * 年内最低价
     */
    @Excel(name = "年内最低价")
    private BigDecimal priceLowYear;

    // -------------------- 链式Setter方法 --------------------

    public StockPriceUs setId(Long id) {
        this.id = id;
        return this;
    }

    public StockPriceUs setCode(String code) {
        this.code = code;
        return this;
    }

    public StockPriceUs setName(String name) {
        this.name = name;
        return this;
    }

    public StockPriceUs setPriceNow(BigDecimal priceNow) {
        this.priceNow = priceNow;
        return this;
    }

    public StockPriceUs setPriceHighDay(BigDecimal priceHighDay) {
        this.priceHighDay = priceHighDay;
        return this;
    }

    public StockPriceUs setPriceLowDay(BigDecimal priceLowDay) {
        this.priceLowDay = priceLowDay;
        return this;
    }

    public StockPriceUs setPriceOpenDay(BigDecimal priceOpenDay) {
        this.priceOpenDay = priceOpenDay;
        return this;
    }

    public StockPriceUs setPriceCloseYesterday(BigDecimal priceCloseYesterday) {
        this.priceCloseYesterday = priceCloseYesterday;
        return this;
    }

    public StockPriceUs setPriceLowWeek(BigDecimal priceLowWeek) {
        this.priceLowWeek = priceLowWeek;
        return this;
    }

    public StockPriceUs setPriceLowYear(BigDecimal priceLowYear) {
        this.priceLowYear = priceLowYear;
        return this;
    }

    // -------------------- 标准Setter/Getter方法 --------------------

    public Long getId() {
        return id;
    }


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


    public BigDecimal getPriceLowWeek() {
        return priceLowWeek;
    }


    public BigDecimal getPriceLowYear() {
        return priceLowYear;
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
    public static StockPriceUs parse(JsonNode node) {
        return new StockPriceUs()
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("code", getCode())
                .append("name", getName())
                .append("priceNow", getPriceNow())
                .append("priceHighDay", getPriceHighDay())
                .append("priceLowDay", getPriceLowDay())
                .append("priceOpenDay", getPriceOpenDay())
                .append("priceCloseYesterday", getPriceCloseYesterday())
                .append("priceLowWeek", getPriceLowWeek())
                .append("priceLowYear", getPriceLowYear())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
