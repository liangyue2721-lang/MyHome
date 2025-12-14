package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import com.make.stock.domain.dto.EtfRealtimeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * ETF交易数据对象 etf_data
 *
 * @author erqi
 * @date 2025-05-28
 */
@Data             // 自动生成getter/setter
@Builder          // 生成构造器模式
@Jacksonized      // 支持Jackson通过Builder反序列化
@AllArgsConstructor
@Accessors(chain = true) // 生成链式set方法
public class EtfData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（建议使用分布式ID生成策略）
     */
    private String id;

    /**
     * ETF唯一标识代码（如：510300）
     */
    private String etfCode;

    /**
     * ETF官方名称（如：沪深300ETF）
     */
    @Excel(name = "ETF名称")
    private String etfName;

    /**
     * 交易日期（精确到日）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date tradeDate;

    // 价格相关字段（单位：元）
    @Excel(name = "开盘价")
    private BigDecimal openPrice;
    @Excel(name = "最高价")
    private BigDecimal highPrice;
    @Excel(name = "最低价")
    private BigDecimal lowPrice;
    @Excel(name = "最新价/收盘价")
    private BigDecimal closePrice;

    // 交易量相关字段
    @Excel(name = "成交量", readConverterExp = "股=")
    private Long volume;
    @Excel(name = "成交额", readConverterExp = "元=")
    private BigDecimal turnover;

    // 实时买卖盘字段
    @Excel(name = "买入价")
    private BigDecimal bidPrice;
    @Excel(name = "卖出价")
    private BigDecimal askPrice;

    // 市场深度数据
    @Excel(name = "外盘成交量")
    private Long externalMarketVolume;
    @Excel(name = "内盘成交量")
    private Long internalMarketVolume;

    // 买一档数据
    @Excel(name = "买1价")
    private BigDecimal buy1Price;
    @Excel(name = "买1量")
    private Long buy1Volume;

    /**
     * 数据来源接口标识
     */
    @Excel(name = "api接口")
    private String stockApi;

    public EtfData() {
    }

    public static EtfData etfRealtimeInfoToEtfData(EtfRealtimeInfo info) {
        EtfData etfData = new EtfData();
        etfData.setEtfCode(info.getStockCode());
        etfData.setEtfName(info.getCompanyName());

        if (info.getOpenPrice() != null)
            etfData.setOpenPrice(BigDecimal.valueOf(info.getOpenPrice()));

        if (info.getHighPrice() != null)
            etfData.setHighPrice(BigDecimal.valueOf(info.getHighPrice()));

        if (info.getLowPrice() != null)
            etfData.setLowPrice(BigDecimal.valueOf(info.getLowPrice()));

        if (info.getPrice() != null)
            etfData.setClosePrice(BigDecimal.valueOf(info.getPrice()));

        etfData.setVolume(info.getVolume());
        if (info.getTurnover() != null)
            etfData.setTurnover(BigDecimal.valueOf(info.getTurnover()));

        return etfData;
    }

    // ============================== 链式方法 ==============================

    public EtfData setId(String id) {
        this.id = id;
        return this;
    }

    public EtfData setEtfCode(String etfCode) {
        this.etfCode = etfCode;
        return this;
    }

    public EtfData setEtfName(String etfName) {
        this.etfName = etfName;
        return this;
    }

    public EtfData setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
        return this;
    }

    public EtfData setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
        return this;
    }

    public EtfData setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
        return this;
    }

    public EtfData setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
        return this;
    }

    public EtfData setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
        return this;
    }

    public EtfData setVolume(Long volume) {
        this.volume = volume;
        return this;
    }

    public EtfData setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
        return this;
    }

    public EtfData setBidPrice(BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
        return this;
    }

    public EtfData setAskPrice(BigDecimal askPrice) {
        this.askPrice = askPrice;
        return this;
    }

    public EtfData setExternalMarketVolume(Long externalMarketVolume) {
        this.externalMarketVolume = externalMarketVolume;
        return this;
    }

    public EtfData setInternalMarketVolume(Long internalMarketVolume) {
        this.internalMarketVolume = internalMarketVolume;
        return this;
    }

    public EtfData setBuy1Price(BigDecimal buy1Price) {
        this.buy1Price = buy1Price;
        return this;
    }

    public EtfData setBuy1Volume(Long buy1Volume) {
        this.buy1Volume = buy1Volume;
        return this;
    }

    public EtfData setStockApi(String stockApi) {
        this.stockApi = stockApi;
        return this;
    }

    // ============================== 工具方法 ==============================

    /**
     * 快速构建方法（示例）
     *
     * <pre>{@code
     * EtfData data = new EtfData()
     *     .setEtfCode("510300")
     *     .setEtfName("沪深300ETF")
     *     .setTradeDate(new Date());
     * }</pre>
     */
    public static EtfData fastCreate(String etfCode, String etfName) {
        return new EtfData()
                .setEtfCode(etfCode)
                .setEtfName(etfName)
                .setTradeDate(new Date());
    }

    public String getId() {
        return id;
    }

    public String getEtfCode() {
        return etfCode;
    }

    public String getEtfName() {
        return etfName;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public Long getVolume() {
        return volume;
    }

    public BigDecimal getTurnover() {
        return turnover;
    }

    public BigDecimal getBidPrice() {
        return bidPrice;
    }

    public BigDecimal getAskPrice() {
        return askPrice;
    }

    public Long getExternalMarketVolume() {
        return externalMarketVolume;
    }

    public Long getInternalMarketVolume() {
        return internalMarketVolume;
    }

    public BigDecimal getBuy1Price() {
        return buy1Price;
    }

    public Long getBuy1Volume() {
        return buy1Volume;
    }

    public String getStockApi() {
        return stockApi;
    }

    /**
     * 安全解析工厂方法（完整版）
     */
    public static EtfData parse(JsonNode node) {
        BigDecimal decimalPoint = new BigDecimal(1000);
        return EtfData.builder()
                .etfCode(safeGetText(node, "f57"))
                .etfName(safeGetText(node, "f58"))
                .tradeDate(new Date())
                .openPrice(BigDecimal.valueOf(parseDouble(node, "f46")).divide(decimalPoint))
                .highPrice(BigDecimal.valueOf(parseDouble(node, "f44")).divide(decimalPoint))
                .lowPrice(BigDecimal.valueOf(parseDouble(node, "f45")).divide(decimalPoint))
                .closePrice(BigDecimal.valueOf(parseDouble(node, "f43")).divide(decimalPoint))
                .volume(safeGetLong(node, "f47"))
                .turnover(BigDecimal.valueOf(parseDouble(node, "f48")).divide(decimalPoint))
                .bidPrice(BigDecimal.valueOf(parseDouble(node, "f31")).divide(decimalPoint))
                .askPrice(BigDecimal.valueOf(parseDouble(node, "f32")).divide(decimalPoint))
                .externalMarketVolume(safeGetLong(node, "f34"))
                .internalMarketVolume(safeGetLong(node, "f35"))
                .buy1Price(BigDecimal.valueOf(parseDouble(node, "f19")).divide(decimalPoint))
                .buy1Volume(safeGetLong(node, "f20"))
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
                .append("id", id)
                .append("etfCode", etfCode)
                .append("etfName", etfName)
                .append("tradeDate", tradeDate)
                .append("openPrice", openPrice)
                .append("highPrice", highPrice)
                .append("lowPrice", lowPrice)
                .append("closePrice", closePrice)
                .append("volume", volume)
                .append("turnover", turnover)
                .append("bidPrice", bidPrice)
                .append("askPrice", askPrice)
                .append("externalMarketVolume", externalMarketVolume)
                .append("internalMarketVolume", internalMarketVolume)
                .append("buy1Price", buy1Price)
                .append("buy1Volume", buy1Volume)
                .append("stockApi", stockApi)
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }

}
