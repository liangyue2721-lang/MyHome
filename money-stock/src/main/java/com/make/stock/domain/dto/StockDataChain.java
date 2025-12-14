package com.make.stock.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * 股票全量数据实体（支持链式存储）
 * <p>
 * 实现特性：
 * 1. 嵌套式链表存储结构
 * 2. 防御式数据解析
 * 3. 构建器模式支持
 * 4. 异常数据自动修复
 * </p>
 */
@Getter
@ToString
@Builder(toBuilder = true)
public class StockDataChain {
    // 基础字段
    @JsonProperty("f12")
    private final String securityCode;   // 证券代码
    @JsonProperty("f14")
    private final String securityName;   // 证券简称
    @JsonProperty("f13")
    private final Integer marketCode;    // 市场编码

    // 实时行情
    @JsonProperty("f2")
    private final BigDecimal latestPrice;    // 最新价
    @JsonProperty("f3")
    private final BigDecimal changePercent; // 涨跌幅
    @JsonProperty("f4")
    private final BigDecimal changeAmount;  // 涨跌额
    @JsonProperty("f1")
    private final Long updateTimestamp;     // 时间戳

    // 量能指标
    @JsonProperty("f5")
    private final Long volume;          // 成交量（手）
    @JsonProperty("f6")
    private final BigDecimal turnover;  // 成交额（元）

    // 链表指针
    private StockDataChain nextNode;  // 下一节点指针
    private StockDataChain prevNode;  // 前一节点指针

    // 扩展字段（需特殊处理）
    @Singular
    private final List<MarketDepth> bidLevels; // 买档位（链式存储）
    @Singular
    private final List<MarketDepth> askLevels; // 卖档位

    // 财务指标
    @JsonProperty("f20")
    private final BigDecimal totalMarketValue;  // 总市值
    @JsonProperty("f21")
    private final BigDecimal circulatingValue;  // 流通市值
    @JsonProperty("f24")
    private final BigDecimal peRatio;           // 市盈率
    @JsonProperty("f25")
    private final BigDecimal pbRatio;           // 市净率

    // 异常状态字段
    private final boolean suspended;       // 停牌状态
    private final boolean stRiskWarning;   // ST风险警示

    /**
     * 五档行情数据结构
     */
    @Getter
    @Builder
    public static class MarketDepth {
        private final BigDecimal price;   // 价格
        private final Long volume;        // 成交量
        private final int level;          // 档位级别
    }

    /**
     * 链式存储操作方法
     */
    public void linkNext(StockDataChain next) {
        this.nextNode = next;
        next.prevNode = this;
    }

    public void linkPrev(StockDataChain prev) {
        this.prevNode = prev;
        prev.nextNode = this;
    }

    /**
     * 安全解析入口（工厂方法）
     */
    public static StockDataChain parse(JsonNode node) {
        return StockDataChain.builder()
                .securityCode(parseCode(node))
                .securityName(parseName(node))
                .marketCode(parseIntField(node, "f13"))
                .latestPrice(parseDecimal(node, "f2"))
                .changePercent(parseDecimal(node, "f3"))
                .changeAmount(parseDecimal(node, "f4"))
                .updateTimestamp(parseLongField(node, "f1"))
                .volume(parseLongField(node, "f5"))
                .turnover(parseDecimal(node, "f6"))
                .bidLevels(parseBidLevels(node))
                .askLevels(parseAskLevels(node))
                .totalMarketValue(parseDecimal(node, "f20"))
                .circulatingValue(parseDecimal(node, "f21"))
                .peRatio(parseDecimal(node, "f24"))
                .pbRatio(parseDecimal(node, "f25"))
                .suspended(parseSuspended(node))
                .stRiskWarning(parseRiskWarning(node))
                .build();
    }

    // ---------- 解析工具方法 ----------
    private static String parseCode(JsonNode node) {
        return Optional.ofNullable(node.path("f12").textValue())
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new DataParseException("Invalid security code"));
    }

    private static String parseName(JsonNode node) {
        return Optional.ofNullable(node.path("f14").textValue())
                .filter(s -> !s.isEmpty())
                .orElse("N/A");
    }

    private static BigDecimal parseDecimal(JsonNode node, String field) {
        try {
            return new BigDecimal(node.path(field).asText().replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static Long parseLongField(JsonNode node, String field) {
        JsonNode val = node.path(field);
        return val.isNumber() ? val.longValue() : 0L;
    }

    private static Integer parseIntField(JsonNode node, String field) {
        JsonNode val = node.path(field);
        return val.isNumber() ? val.intValue() : 0;
    }

    private static List<MarketDepth> parseBidLevels(JsonNode node) {
        List<MarketDepth> levels = new LinkedList<>();
        // 解析买1-买5档
        for (int i = 1; i <= 5; i++) {
            levels.add(MarketDepth.builder()
                    .price(parseDecimal(node, "f1" + (i + 6)))
                    .volume(parseLongField(node, "f2" + (i + 6)))
                    .level(i)
                    .build());
        }
        return levels;
    }

    private static List<MarketDepth> parseAskLevels(JsonNode node) {
        List<MarketDepth> levels = new LinkedList<>();
        // 解析卖1-卖5档
        for (int i = 1; i <= 5; i++) {
            levels.add(MarketDepth.builder()
                    .price(parseDecimal(node, "f1" + (i + 1)))
                    .volume(parseLongField(node, "f2" + (i + 1)))
                    .level(i)
                    .build());
        }
        return levels;
    }

    private static boolean parseSuspended(JsonNode node) {
        return node.path("f62").decimalValue().compareTo(BigDecimal.ZERO) == 0;
    }

    private static boolean parseRiskWarning(JsonNode node) {
        return node.path("f136").asText().contains("ST");
    }

    // ---------- 链式操作扩展 ----------
    public StockDataChain findHead() {
        StockDataChain current = this;
        while (current.prevNode != null) {
            current = current.prevNode;
        }
        return current;
    }

    public StockDataChain findTail() {
        StockDataChain current = this;
        while (current.nextNode != null) {
            current = current.nextNode;
        }
        return current;
    }

    public void insertAfter(StockDataChain newNode) {
        StockDataChain oldNext = this.nextNode;
        this.nextNode = newNode;
        newNode.prevNode = this;
        newNode.nextNode = oldNext;
        if (oldNext != null) {
            oldNext.prevNode = newNode;
        }
    }

    /**
     * 数据验证异常
     */
    public static class DataParseException extends RuntimeException {
        public DataParseException(String message) {
            super(message);
        }
    }
}