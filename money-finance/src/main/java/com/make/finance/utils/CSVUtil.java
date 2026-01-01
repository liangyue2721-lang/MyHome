package com.make.finance.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.make.finance.domain.TransactionRecords;
import com.make.finance.domain.dto.AliPayment;
import com.make.finance.domain.dto.WeChatTransaction;
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
     * @param file 上传的文件 (支持 xls, xlsx, csv)
     * @return 解析后的标准交易记录列表
     */
    public static List<TransactionRecords> parse(File file) {
        UniversalListener listener = new UniversalListener();
        try {
            // EasyExcel 自动识别格式 (包括 CSV)
            EasyExcel.read(file, listener).sheet().doRead();
        } catch (Exception e) {
            log.error("解析文件失败: {}", file.getName(), e);
            // 如果 EasyExcel 失败（例如编码问题），可以考虑备用方案，但需求已确认统一使用 EasyExcel
        }
        return listener.getRecords();
    }

    /**
     * 内部监听器，实现状态机逻辑
     */
    public static class UniversalListener extends AnalysisEventListener<Map<Integer, String>> {
        private final List<TransactionRecords> records = new ArrayList<>();
        private boolean headerFound = false;
        private TransactionType detectedType = null;
        private Map<String, Integer> headerMap = new HashMap<>();

        @Override
        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
            // 1. 寻找表头
            if (!headerFound) {
                if (isHeaderRow(rowData)) {
                    headerFound = true;
                    // 根据表头特征确定类型
                    detectedType = identifyType(rowData);
                    // 建立列名到索引的映射
                    buildHeaderMap(rowData);
                    log.info("找到表头，识别类型: {}", detectedType);
                }
                return;
            }

            // 2. 解析数据行
            if (detectedType != null) {
                try {
                    TransactionRecords record = parseRow(rowData, detectedType);
                    if (record != null) {
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
            // 核心特征：必须包含 "交易时间"
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
                if (value.contains("支付宝交易号") || value.contains("交易订单号")) return TransactionType.ALIPAY;
            }
            // 默认为微信（兼容旧逻辑）或抛出异常
            return TransactionType.WECHAT;
        }

        private void buildHeaderMap(Map<Integer, String> row) {
            headerMap.clear();
            for (Map.Entry<Integer, String> entry : row.entrySet()) {
                if (entry.getValue() != null) {
                    // 归一化表头：去除空格，统一名称
                    String key = entry.getValue().trim();
                    headerMap.put(key, entry.getKey());
                }
            }
        }

        private TransactionRecords parseRow(Map<Integer, String> row, TransactionType type) {
            // 简单校验：如果没有交易时间，视为无效行
            Integer timeIdx = findColumnIndex("交易时间");
            if (timeIdx == null || row.get(timeIdx) == null) return null;

            TransactionRecords record = new TransactionRecords();

            // 通用字段解析
            record.setTransactionTime(parseDate(row.get(timeIdx)));

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

            // 类型特定设置
            if (type == TransactionType.WECHAT) {
                record.setSource("微信");
            } else if (type == TransactionType.ALIPAY) {
                record.setSource("支付宝");
            }

            // 默认 ProductType (后续 Controller 还有业务逻辑覆盖)
            record.setProductType(record.getTransactionType());

            return record;
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
                // 尝试多种格式
                dateStr = dateStr.trim();
                if (dateStr.contains(":")) {
                     return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
                }
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            } catch (Exception e) {
                // EasyExcel 有时会读取为 Excel 数字日期格式，此处简化处理，假设为字符串
                return null;
            }
        }

        private BigDecimal parseAmount(String amountStr) {
            if (amountStr == null || amountStr.isEmpty()) return BigDecimal.ZERO;
            try {
                // 移除 ¥, 元 等符号
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
