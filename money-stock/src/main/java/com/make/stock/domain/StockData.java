package com.make.stock.domain;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.make.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * 股票数据对象 stock_data
 * 用于存储和匹配以下JSON格式:
 * {
 *   "amount": 653438249.0,
 *   "change": -0.02,
 *   "change_pct": -0.35,
 *   "close": 5.75,
 *   "high": 5.78,
 *   "low": 5.74,
 *   "open": 5.76,
 *   "pre_close": 5.77,
 *   "stock_code": "601006",
 *   "trade_date": "2025-10-30",
 *   "trade_time": "2025-10-30 00:00:00",
 *   "turnover_ratio": 0.56,
 *   "volume": 113476300
 * }
 *
 * @author make
 * @date 2025-10-30
 */
@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StockData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 成交额 - 股票成交的总金额，单位：元
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * 涨跌额 - 当前价格相对于前收盘价的变动金额
     */
    @JsonProperty("change")
    private BigDecimal change;

    /**
     * 涨跌幅 - 当前价格相对于前收盘价的变动百分比
     */
    @JsonProperty("change_pct")
    private BigDecimal changePct;

    /**
     * 收盘价 - 当日收盘价格
     */
    @JsonProperty("close")
    private BigDecimal close;

    /**
     * 最高价 - 当日最高成交价格
     */
    @JsonProperty("high")
    private BigDecimal high;

    /**
     * 最低价 - 当日最低成交价格
     */
    @JsonProperty("low")
    private BigDecimal low;

    /**
     * 开盘价 - 当日开盘价格
     */
    @JsonProperty("open")
    private BigDecimal open;

    /**
     * 前收盘价 - 上一个交易日的收盘价格
     */
    @JsonProperty("pre_close")
    private BigDecimal preClose;

    /**
     * 股票代码 - 股票的唯一标识代码
     */
    @JsonProperty("stock_code")
    private String stockCode;

    /**
     * 交易日期 - 数据对应的日期
     */
    @JsonFormat(pattern = DATE_FORMAT)
    @JsonProperty("trade_date")
    private Date tradeDate;

    /**
     * 交易时间 - 数据产生的时间戳
     */
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    @JsonProperty("trade_time")
    private Date tradeTime;

    /**
     * 换手率 - 股票转手买卖的频率，百分比
     */
    @JsonProperty("turnover_ratio")
    private BigDecimal turnoverRatio;

    /**
     * 成交量 - 股票成交的总数量，单位：股
     */
    @JsonProperty("volume")
    private Long volume;

    // ============================== 业务方法 ==============================

    /**
     * 计算总市值（需要外部传入总股本）
     */
    public BigDecimal calculateMarketCap(BigDecimal totalShares) {
        if (close == null || totalShares == null) {
            return null;
        }
        return close.multiply(totalShares);
    }

    /**
     * 检查是否是涨停
     */
    public boolean isLimitUp() {
        if (preClose == null || changePct == null) {
            return false;
        }
        // 假设涨跌幅超过9.9%为涨停
        return changePct.compareTo(new BigDecimal("9.9")) >= 0;
    }

    /**
     * 检查是否是跌停
     */
    public boolean isLimitDown() {
        if (preClose == null || changePct == null) {
            return false;
        }
        // 假设涨跌幅低于-9.9%为跌停
        return changePct.compareTo(new BigDecimal("-9.9")) <= 0;
    }

    /**
     * 获取振幅（(最高价-最低价)/前收盘价）
     */
    public BigDecimal getAmplitude() {
        if (preClose == null || high == null || low == null || preClose.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return high.subtract(low).divide(preClose, 4, BigDecimal.ROUND_HALF_UP);
    }

    // ============================== 工具方法 ==============================

    /**
     * 快速构建方法
     */
    public static StockData fastCreate(String stockCode) {
        return new StockData()
                .setStockCode(stockCode)
                .setTradeDate(new Date())
                .setTradeTime(new Date());
    }

    /**
     * 安全解析工厂方法 - 增强版本
     */
    public static StockData parse(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        try {
            return StockData.builder()
                    .amount(safeGetBigDecimal(node, "amount"))
                    .change(safeGetBigDecimal(node, "change"))
                    .changePct(safeGetBigDecimal(node, "change_pct"))
                    .close(safeGetBigDecimal(node, "close"))
                    .high(safeGetBigDecimal(node, "high"))
                    .low(safeGetBigDecimal(node, "low"))
                    .open(safeGetBigDecimal(node, "open"))
                    .preClose(safeGetBigDecimal(node, "pre_close"))
                    .stockCode(safeGetText(node, "stock_code"))
                    .tradeDate(safeGetDate(node, "trade_date", DATE_FORMAT))
                    .tradeTime(safeGetDate(node, "trade_time", DATE_TIME_FORMAT))
                    .turnoverRatio(safeGetBigDecimal(node, "turnover_ratio"))
                    .volume(safeGetLong(node, "volume"))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("解析股票数据失败: " + e.getMessage(), e);
        }
    }

    // ============================== 私有辅助方法 ==============================

    private static BigDecimal safeGetBigDecimal(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isNumber()) {
            return valueNode.decimalValue();
        } else if (valueNode.isTextual()) {
            try {
                return new BigDecimal(valueNode.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String safeGetText(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private static Long safeGetLong(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isNumber()) {
            return valueNode.asLong();
        } else if (valueNode.isTextual()) {
            try {
                return Long.parseLong(valueNode.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Date safeGetDate(JsonNode node, String fieldName, String pattern) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        try {
            String dateStr = node.get(fieldName).asText();
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            return formatter.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    // ============================== 验证方法 ==============================

    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        return stockCode != null && !stockCode.trim().isEmpty()
                && tradeDate != null
                && close != null;
    }

    /**
     * 验证是否为有效交易数据（包含必要的价格和成交量信息）
     */
    public boolean isValidTransaction() {
        return isValid()
                && volume != null && volume > 0
                && amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // ============================== 链式方法 ==============================

    public StockData setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public StockData setChange(BigDecimal change) {
        this.change = change;
        return this;
    }

    public StockData setChangePct(BigDecimal changePct) {
        this.changePct = changePct;
        return this;
    }

    public StockData setClose(BigDecimal close) {
        this.close = close;
        return this;
    }

    public StockData setHigh(BigDecimal high) {
        this.high = high;
        return this;
    }

    public StockData setLow(BigDecimal low) {
        this.low = low;
        return this;
    }

    public StockData setOpen(BigDecimal open) {
        this.open = open;
        return this;
    }

    public StockData setPreClose(BigDecimal preClose) {
        this.preClose = preClose;
        return this;
    }

    public StockData setStockCode(String stockCode) {
        this.stockCode = stockCode;
        return this;
    }

    public StockData setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
        return this;
    }

    public StockData setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
        return this;
    }

    public StockData setTurnoverRatio(BigDecimal turnoverRatio) {
        this.turnoverRatio = turnoverRatio;
        return this;
    }

    public StockData setVolume(Long volume) {
        this.volume = volume;
        return this;
    }
}
