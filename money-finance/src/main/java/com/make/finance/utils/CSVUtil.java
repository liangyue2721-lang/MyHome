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
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 统一流水文件解析工具类 (Universal Transaction Parser)
 * <p>
 * 支持微信、支付宝等格式的统一解析。
 * 核心逻辑：不依赖文件名或固定行号，而是扫描内容定位表头。
 * </p>
 *
 * @author 84522
 */
public class CSVUtil {

    private static final Logger log = LoggerFactory.getLogger(CSVUtil.class);

    /**
     * 统一解析方法
     * 自动识别文件类型（微信/支付宝）并解析为标准交易记录
     *
     * @param file   上传的文件 (支持 xls, xlsx, csv)
     * @param userId 用户ID，用于填充记录归属
     * @return 解析后的标准交易记录列表
     */
    public static List<TransactionRecords> parse(File file, Long userId) {
        UniversalListener listener = new UniversalListener(userId);
        try {
            // EasyExcel 自动识别格式 (包括 CSV)
            EasyExcel.read(file, listener).sheet().doRead();
        } catch (Exception e) {
            log.error("解析文件失败: {}", file.getName(), e);
        }
        return listener.getRecords();
    }

    /**
     * 内部监听器，实现状态机逻辑
     */
    public static class UniversalListener extends AnalysisEventListener<Map<Integer, String>> {
        private final List<TransactionRecords> records = new ArrayList<>();
        private final Long userId;
        private boolean headerFound = false;
        private TransactionType detectedType = null;
        private Map<String, Integer> headerMap = new HashMap<>();

        public UniversalListener(Long userId) {
            this.userId = userId;
        }

        @Override
        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
            // 1. 寻找表头
            if (!headerFound) {
                if (isHeaderRow(rowData)) {
                    headerFound = true;
                    detectedType = identifyType(rowData);
                    buildHeaderMap(rowData);
                    log.info("找到表头，识别类型: {}", detectedType);
                }
                return;
            }

            // 2. 解析数据行
            if (detectedType != null) {
                try {
                    TransactionRecords record = parseRow(rowData, detectedType);
                    // 只有当记录有效且核心字段（如时间）不为空时才添加
                    if (record != null && record.getTransactionTime() != null) {
                        enrichRecord(record); // 3. 数据清洗与分类
                        records.add(record);
                    }
                } catch (Exception e) {
                    log.warn("行解析失败，跳过: {}", rowData, e);
                }
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            log.info("解析完成，共提取有效记录 {} 条", records.size());
        }

        public List<TransactionRecords> getRecords() {
            return records;
        }

        // --- 辅助逻辑 ---

        private boolean isHeaderRow(Map<Integer, String> row) {
            for (String value : row.values()) {
                if (value != null && value.contains("交易时间")) {
                    return true;
                }
            }
            return false;
        }

        private TransactionType identifyType(Map<Integer, String> headerRow) {
            for (String value : headerRow.values()) {
                if (value == null) continue;
                if (value.contains("微信支付单号")) return TransactionType.WECHAT;
                if (value.contains("支付宝交易号") || value.contains("交易订单号") || value.contains("交易号")) return TransactionType.ALIPAY;
            }
            return TransactionType.WECHAT;
        }

        private void buildHeaderMap(Map<Integer, String> row) {
            headerMap.clear();
            for (Map.Entry<Integer, String> entry : row.entrySet()) {
                if (entry.getValue() != null) {
                    String key = entry.getValue().trim();
                    headerMap.put(key, entry.getKey());
                }
            }
        }

        private TransactionRecords parseRow(Map<Integer, String> row, TransactionType type) {
            Integer timeIdx = findColumnIndex("交易时间");
            if (timeIdx == null) return null;

            String timeStr = row.get(timeIdx);
            if (timeStr == null || timeStr.trim().isEmpty()) return null;

            Date transactionTime = parseDate(timeStr);
            // 如果时间解析失败，说明这可能不是有效的数据行（例如尾部的统计行）
            if (transactionTime == null) return null;

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

            if (type == TransactionType.WECHAT) {
                record.setSource("微信");
            } else if (type == TransactionType.ALIPAY) {
                record.setSource("支付宝");
            }

            return record;
        }

        /**
         * 完善记录信息：设置用户ID、分类商品类型
         */
        private void enrichRecord(TransactionRecords record) {
            record.setUserId(userId);

            // 确保不为 null，避免空指针
            String product = record.getProduct() != null ? record.getProduct() : "";
            String type = record.getTransactionType() != null ? record.getTransactionType() : "";
            record.setProduct(product);
            record.setTransactionType(type);

            // 默认分类
            record.setProductType(type);

            // 自动分类逻辑
            if (product.contains("一卡通充值") || product.contains("12306消费") || type.contains("滴滴") || type.contains("中铁")) {
                record.setProductType("交通出行");
            } else if (product.contains("衣") || product.contains("口红") || product.contains("唇")
                    || product.contains("靴") || product.contains("唯品会") || type.contains("唯品会")) {
                record.setProductType("服饰装扮");
            } else if (product.contains("扫二维码") || type.contains("平台商户") || type.contains("拼多多")) {
                record.setProductType("商户消费");
            } else if (product.contains("交费")) {
                record.setProductType("手机话费");
            } else if (product.contains("租房订单")) {
                record.setProductType("租房费用");
            } else if (product.contains("转账") || product.contains("红包") || type.contains("转账") || type.contains("红包")) {
                record.setProductType("转账");
            }
        }

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

        private String getValue(Map<Integer, String> row, String... keywords) {
            Integer index = findColumnIndex(keywords);
            if (index != null) {
                String val = row.get(index);
                return val != null ? val.trim() : "";
            }
            return "";
        }

        private Date parseDate(String dateStr) {
            if (dateStr == null) return null;
            try {
                dateStr = dateStr.trim();
                // 支持 "2025-12-31 20:29:36" (标准) 和 "2025-12-22 4:08:34" (单位数时间)
                // SimpleDateFormat 默认 lenient=true，可以解析单个数字的月/日/时/分
                if (dateStr.contains(":")) {
                     return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
                }
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            } catch (Exception e) {
                // 如果解析失败，返回 null，由调用方决定忽略该行
                return null;
            }
        }

        private BigDecimal parseAmount(String amountStr) {
            if (amountStr == null || amountStr.isEmpty()) return BigDecimal.ZERO;
            try {
                String cleaned = amountStr.replaceAll("[^0-9\\.-]", "");
                return new BigDecimal(cleaned).setScale(6, RoundingMode.HALF_UP);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
    }

    private enum TransactionType {
        WECHAT, ALIPAY
    }
}
