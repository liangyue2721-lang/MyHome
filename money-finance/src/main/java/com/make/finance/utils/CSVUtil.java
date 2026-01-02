package com.make.finance.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.make.finance.domain.TransactionRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统一流水文件解析工具类
 *
 * <p>
 * 功能：
 * <ul>
 *     <li>支持微信 / 支付宝流水 Excel / CSV 导入</li>
 *     <li>不依赖固定行号，自动扫描表头</li>
 *     <li>对非法行、统计行、格式异常具备容错能力</li>
 * </ul>
 * </p>
 *
 * <p>
 * 设计重点：
 * <ul>
 *     <li>流式解析（适合大文件）</li>
 *     <li>强可观测性（明确知道为什么丢行）</li>
 * </ul>
 * </p>
 */
public class CSVUtil {

    private static final Logger log = LoggerFactory.getLogger(CSVUtil.class);

    /**
     * 解析入口
     *
     * @param file   用户上传文件
     * @param userId 当前用户 ID
     * @return 成功解析的交易记录
     */
    public static List<TransactionRecords> parse(File file, Long userId) {
        UniversalListener listener = new UniversalListener(userId);
        try {
            EasyExcel.read(file, listener).sheet().doRead();
        } catch (Exception e) {
            log.error("文件解析失败: {}", file.getName(), e);
        }
        return listener.getRecords();
    }

    /**
     * 通用监听器（核心状态机）
     */
    public static class UniversalListener extends AnalysisEventListener<Map<Integer, String>> {

        private final List<TransactionRecords> records = new ArrayList<>();
        private final Long userId;

        /**
         * 是否已经识别到表头
         */
        private boolean headerFound = false;

        /**
         * 当前流水来源类型
         */
        private TransactionType detectedType;

        /**
         * 表头名 → 列索引
         */
        private final Map<String, Integer> headerMap = new HashMap<>();

        /**
         * 行统计（用于诊断丢行原因）
         */
        private int totalRows = 0;
        private int droppedByTime = 0;
        private int droppedByHeader = 0;

        public UniversalListener(Long userId) {
            this.userId = userId;
        }

        /**
         * 每读取一行都会进入此方法
         */
        @Override
        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
            totalRows++;

            // ========= 阶段 1：寻找表头 =========
            if (!headerFound) {
                if (isHeaderRow(rowData)) {
                    headerFound = true;
                    detectedType = identifyType(rowData);
                    buildHeaderMap(rowData);

                    log.info("识别到表头，交易类型={}, 表头映射={}", detectedType, headerMap);
                }
                return; // 表头前的说明行全部忽略（设计行为）
            }

            // ========= 阶段 2：解析数据行 =========
            try {
                TransactionRecords record = parseRow(rowData, detectedType);

                if (record == null) {
                    return; // 已在内部记录日志
                }

                enrichRecord(record);
                records.add(record);

            } catch (Exception e) {
                log.warn("行解析异常，已跳过: {}", rowData, e);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            log.info(
                    "解析完成：总行数={}, 成功={}, 时间丢弃={}, 表头丢弃={}",
                    totalRows,
                    records.size(),
                    droppedByTime,
                    droppedByHeader
            );
        }

        public List<TransactionRecords> getRecords() {
            return records;
        }

        // ========================= 表头识别 =========================

        /**
         * 判断是否为表头行
         */
        private boolean isHeaderRow(Map<Integer, String> row) {
            return row.values().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(v -> v.contains("交易时间"));
        }

        /**
         * 根据关键字段判断微信 / 支付宝
         */
        private TransactionType identifyType(Map<Integer, String> headerRow) {
            for (String value : headerRow.values()) {
                if (value == null) continue;
                if (value.contains("微信支付单号")) return TransactionType.WECHAT;
                if (value.contains("支付宝交易号") || value.contains("交易订单号") || value.contains("交易号")) {
                    return TransactionType.ALIPAY;
                }
            }
            return TransactionType.WECHAT;
        }

        /**
         * 构建表头映射
         */
        private void buildHeaderMap(Map<Integer, String> row) {
            headerMap.clear();
            row.forEach((k, v) -> {
                if (v != null) headerMap.put(v.trim(), k);
            });
        }

        // ========================= 行解析 =========================

        /**
         * 将一行原始数据解析为交易记录
         */
        private TransactionRecords parseRow(Map<Integer, String> row, TransactionType type) {

            // 1️⃣ 查找时间列
            Integer timeIdx = findColumnIndex("交易时间", "支付时间", "创建时间");
            if (timeIdx == null) {
                droppedByHeader++;
                log.debug("未找到时间列，行已跳过: {}", row);
                return null;
            }

            String timeStr = row.get(timeIdx);
            if (timeStr == null || timeStr.isBlank()) {
                droppedByTime++;
                log.debug("时间为空，行已跳过: {}", row);
                return null;
            }

            // 2️⃣ 时间解析（核心丢行点）
            Date transactionTime = parseDate(timeStr);
            if (transactionTime == null) {
                droppedByTime++;
                log.warn("时间解析失败 [{}]，行已跳过", timeStr);
                return null;
            }

            // 3️⃣ 构建记录
            TransactionRecords record = new TransactionRecords();
            record.setTransactionTime(transactionTime);
            record.setTransactionType(getValue(row, "交易类型", "交易分类"));
            record.setCounterparty(getValue(row, "交易对方", "交易对象"));
            record.setProduct(getValue(row, "商品", "商品说明", "商品名称"));
            record.setInOut(getValue(row, "收/支", "收支"));
            record.setAmount(parseAmount(getValue(row, "金额(元)", "金额")));
            record.setPaymentMethod(getValue(row, "支付方式", "收/付款方式"));
            record.setTransactionStatus(getValue(row, "当前状态", "交易状态"));
            record.setTransactionId(getValue(row, "交易单号", "交易订单号"));
            record.setMerchantId(getValue(row, "商户单号", "商家订单号"));
            record.setNote(getValue(row, "备注"));
            record.setSource(type == TransactionType.WECHAT ? "微信" : "支付宝");

            return record;
        }

        // ========================= 数据清洗 =========================

        /**
         * 填充用户信息 + 自动分类
         */
        private void enrichRecord(TransactionRecords record) {
            record.setUserId(userId);

            String product = Optional.ofNullable(record.getProduct()).orElse("");
            String type = Optional.ofNullable(record.getTransactionType()).orElse("");

            record.setProduct(product);
            record.setTransactionType(type);
            record.setProductType(type);

            if (product.contains("一卡通") || type.contains("滴滴") || type.contains("中铁")) {
                record.setProductType("交通出行");
            } else if (product.contains("衣") || product.contains("唯品会")) {
                record.setProductType("服饰装扮");
            } else if (type.contains("拼多多")) {
                record.setProductType("商户消费");
            } else if (product.contains("交费")) {
                record.setProductType("手机话费");
            } else if (product.contains("租房")) {
                record.setProductType("租房费用");
            } else if (product.contains("转账") || product.contains("红包")) {
                record.setProductType("转账");
            }
        }

        // ========================= 工具方法 =========================

        /**
         * 模糊匹配表头列
         */
        private Integer findColumnIndex(String... keywords) {
            for (String keyword : keywords) {
                for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                    if (entry.getKey().contains(keyword)) {
                        return entry.getValue();
                    }
                }
            }
            return null;
        }

        /**
         * 安全获取列值
         */
        private String getValue(Map<Integer, String> row, String... keywords) {
            Integer idx = findColumnIndex(keywords);
            return idx == null ? "" : Optional.ofNullable(row.get(idx)).orElse("").trim();
        }

        /**
         * 多格式时间解析（增强版，支持 1~2 位 月 / 日 / 时）
         * <p>
         * 支持示例：
         * - 2025-11-7 14:52
         * - 2025-1-7 14:52
         * - 2025-1-17 14:52
         * - 2025-01-07 04:52:34
         * - 2025-1-7
         */
        private Date parseDate(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            String v = value.trim();

            try {
                // ========= 1️⃣ yyyy-M-d H:mm:ss =========
                if (v.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}")) {
                    LocalDateTime ldt = LocalDateTime.parse(
                            v,
                            DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss")
                    );
                    return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                }

                // ========= 2️⃣ yyyy-M-d H:mm =========
                if (v.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}")) {
                    LocalDateTime ldt = LocalDateTime.parse(
                            v,
                            DateTimeFormatter.ofPattern("yyyy-M-d H:mm")
                    ).withSecond(0);
                    return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                }

                // ========= 3️⃣ yyyy-M-d =========
                if (v.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                    LocalDate ld = LocalDate.parse(
                            v,
                            DateTimeFormatter.ofPattern("yyyy-M-d")
                    );
                    return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
                }

            } catch (Exception e) {
                // 任何解析异常都视为非法时间
                return null;
            }

            return null;
        }


        /**
         * 金额解析（失败返回 0）
         */
        private BigDecimal parseAmount(String amountStr) {
            if (amountStr == null || amountStr.isBlank()) return BigDecimal.ZERO;
            try {
                String cleaned = amountStr.replaceAll("[^0-9\\.-]", "");
                return new BigDecimal(cleaned).setScale(6, RoundingMode.HALF_UP);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * 交易来源枚举
     */
    private enum TransactionType {
        WECHAT, ALIPAY
    }
}
