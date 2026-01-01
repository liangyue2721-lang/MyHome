package com.make.finance.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.make.finance.domain.dto.AliPayment;
import com.make.finance.domain.dto.WeChatTransaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV文件解析工具类
 * <p>
 * 提供对支付宝、微信等支付平台导出的CSV文件进行解析的功能，
 * 以及生成和写入CSV文件的相关方法
 * </p>
 *
 * @author 84522
 */
public class CSVUtil {

    // 日志记录器，用于记录解析过程中的日志信息
    private static final Logger log = LoggerFactory.getLogger(CSVUtil.class);

    /**
     * 支付宝流水表单开始位置标识
     * <p>
     * 用于标识支付宝CSV文件中数据开始的位置
     * </p>
     */
    public static final String ALIPAY_CSV_HEADER = "------------------------支付宝（中国）网络技术有限公司  电子客户回单------------------------";

    /**
     * 微信流水表单开始位置标识
     * <p>
     * 用于标识微信CSV文件中数据开始的位置
     * </p>
     */
    public static final String WECHAT_CSV_HEADER = "----------------------微信支付账单明细列表--------------------";

    /**
     * 使用EasyExcel解析微信支付CSV文件
     * <p>
     * 通过EasyExcel框架读取微信支付导出的CSV文件，并将其转换为WeChatTransaction对象列表
     * </p>
     *
     * @param file 微信支付CSV文件
     * @return 包含解析后微信交易记录的列表
     */
    public static List<WeChatTransaction> easyExcelParseWeChatCsv(File file) {
        // 创建用于存储解析结果的列表
        List<WeChatTransaction> records = new ArrayList<>();
        try {
            // 使用EasyExcel读取文件
            EasyExcel.read(file, WeChatTransaction.class, new AnalysisEventListener<WeChatTransaction>() {
                // 标记是否找到表头
                private boolean headerFound = false;

                /**
                 * 每解析一行数据时调用该方法
                 *
                 * @param record  当前行解析出的数据对象
                 * @param context 解析上下文信息
                 */
                @Override
                public void invoke(WeChatTransaction record, AnalysisContext context) {
                    if (!headerFound) {
                        // 检查当前行是否包含关键字段 "交易时间"
                        // 这里的 record 是根据 Excel 内容映射的对象，如果是非数据行，字段可能错位或包含表头文本
                        if (isWeChatHeader(record)) {
                            headerFound = true;
                        }
                        return;
                    }

                    // 找到表头后，后续行视为有效数据
                    // 简单的有效性检查，例如交易时间不能为空
                    if (record.getTransactionTime() != null && !record.getTransactionTime().trim().isEmpty()) {
                        records.add(record);
                    }
                }

                private boolean isWeChatHeader(WeChatTransaction record) {
                    // 检查各个字段是否包含表头关键字
                    // EasyExcel 读取时，如果当前行是表头行，它会尝试映射到字段中
                    // 我们检查第一个字段（交易时间）是否包含 "交易时间"
                    return (record.getTransactionTime() != null && record.getTransactionTime().contains("交易时间"))
                            || (record.getTransactionType() != null && record.getTransactionType().contains("交易类型"));
                }

                /**
                 * 所有数据解析完成后调用该方法
                 *
                 * @param context 解析上下文信息
                 */
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 处理完成后的逻辑
                    log.info("微信流水解析完成，共{}条记录", records.size());
                }
            }).sheet().doRead();
        } catch (Exception e) {
            // 记录解析异常日志
            log.error("微信流水解析异常：{}", e.getMessage(), e);
        }

        // 返回解析结果
        return records;
    }


    /**
     * 解析支付宝流水的CSV文件，并将每行数据转换为Payment对象列表
     * <p>
     * 使用 Apache Commons CSV 进行健壮解析
     * </p>
     *
     * @param filePath CSV文件路径
     * @return 包含解析后Payment对象的列表
     */
    public static List<AliPayment> parseAlipayCsv(String filePath) {
        return easyExcelParseAlipayCsv(new File(filePath));
    }

    /**
     * 使用 Apache Commons CSV 解析支付宝支付CSV文件
     * <p>
     * 读取支付宝支付导出的CSV文件，并将其转换为AliPayment对象列表
     * 动态查找表头，不依赖固定行号
     * </p>
     *
     * @param file 支付宝支付CSV文件
     * @return 包含解析后支付宝支付记录的列表
     */
    public static List<AliPayment> easyExcelParseAlipayCsv(File file) {
        List<AliPayment> payments = new ArrayList<>();

        // 支付宝导出通常是 GBK 编码
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"))) {

            // 1. 动态查找表头行
            String line;
            boolean headerFound = false;

            // 预读取 reader 直到找到表头
            // 注意：Commons CSV 的 parser 需要从表头行开始解析，或者我们需要手动处理
            // 这里我们先手动读取直到找到包含 "交易时间" 的行

            // 为了让 CSVParser 能从正确的位置开始，我们使用一个 StringBuilder 来收集从表头开始的内容
            // 或者更简单：找到表头后，将表头行和剩余内容传递给 CSVParser

            // 重新思考：BufferedReader 是流式的。如果我们读了一部分，剩下的可以直接传给 CSVParser 吗？
            // Commons CSV Parser 可以接受 Reader。如果 Reader 已经前进到了数据行，Parser 会从那里开始。
            // 但是 Parser 通常需要表头来映射。

            // 方案：
            // 逐行读取，直到找到 "交易时间" 开头的行。
            // 这一行是 Header。
            // 下一行开始是数据。

            while ((line = reader.readLine()) != null) {
                if (line.contains("交易时间") && line.contains("交易类型") && line.contains("交易对方")) {
                    headerFound = true;
                    break;
                }
            }

            if (!headerFound) {
                log.warn("未找到支付宝流水的表头行，无法解析文件: {}", file.getName());
                return payments;
            }

            // 2. 使用 CSVParser 解析剩余内容
            // 支付宝 CSV 通常以制表符或逗号分隔。如果是 excel 导出的 csv，通常是逗号。
            // 截图显示是 CSV，且之前代码用 split("\t")，暗示可能是制表符？
            // 但用户提供的截图看起来像标准 Excel 打开的 CSV。支付宝历史版本确实用过制表符。
            // 不过 Commons CSV 可以自动探测或者我们尝试两种。
            // 根据之前的代码 split("\t")，我们优先尝试制表符，或者 CSV 格式。
            // 标准支付宝导出（手机端/网页端）通常是 CSV 格式（逗号分隔）。
            // 如果之前的代码用 split("\t") 能跑（尽管现在要重构），说明可能是制表符。
            // 但用户说 "支付宝流水格式为 csv... 之前代码 split('\t')".
            // 如果用户确认是 CSV，那应该是逗号。
            // 为了保险，我们可以检测行中的分隔符，或者直接使用 DEFAULT (逗号)。
            // 这里的 line 是表头行。

            CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .build();

            // 如果表头行包含制表符而不包含逗号，则切换为 TDF
            if (line.contains("\t") && !line.contains(",")) {
                format = CSVFormat.TDF.builder()
                    .setIgnoreSurroundingSpaces(true)
                    .setIgnoreEmptyLines(true)
                    .build();
            }

            // 我们需要解析这一行表头来确定列的映射，或者直接假设列顺序
            // 简单起见，且之前的代码是按索引获取，我们这里也按索引获取，但基于 CSVParser

            // 创建 Parser，传入 reader（此时 reader 指针在表头之后）
            try (CSVParser parser = new CSVParser(reader, format)) {
                 for (CSVRecord record : parser) {
                     // 支付宝有些尾部说明行，列数很少，跳过
                     if (record.size() < 5) {
                         continue;
                     }

                     // 之前的代码逻辑映射：
                     // 0:交易时间 1:交易分类 2:交易对方 3:对方账号 4:商品说明 5:收/支 6:金额 7:收/付款方式 8:交易状态 9:交易订单号 10:商家订单号 11:备注

                     AliPayment payment = new AliPayment();
                     try {
                         payment.setTransactionTime(getRecordValue(record, 0));
                         payment.setTransactionType(getRecordValue(record, 1));
                         payment.setCounterparty(getRecordValue(record, 2));
                         payment.setCounterpartyAccount(getRecordValue(record, 3));
                         payment.setProductDescription(getRecordValue(record, 4));
                         payment.setInOut(getRecordValue(record, 5));
                         payment.setAmount(getRecordValue(record, 6));
                         payment.setPaymentMethod(getRecordValue(record, 7));
                         payment.setTransactionStatus(getRecordValue(record, 8));
                         payment.setTransactionOrderId(getRecordValue(record, 9));
                         payment.setMerchantOrderId(getRecordValue(record, 10));
                         payment.setNote(getRecordValue(record, 11));

                         payments.add(payment);
                     } catch (Exception e) {
                         // 忽略单行解析错误
                         log.warn("解析支付宝流水行失败: {}", record.toString());
                     }
                 }
            }

            log.info("支付宝流水解析完成，共{}条记录", payments.size());

        } catch (IOException e) {
            log.error("解析支付宝CSV文件异常", e);
        }

        return payments;
    }

    private static String getRecordValue(CSVRecord record, int index) {
        if (index < record.size()) {
            return record.get(index).trim();
        }
        return "";
    }


    /**
     * 解析招商银行CSV文件，并返回AliPayment对象列表
     * <p>
     * 通过传统方式逐行读取招商银行导出的CSV文件，并将其转换为AliPayment对象列表
     * </p>
     *
     * @param filePath 招商银行CSV文件的路径
     * @return 包含招商银行支付信息的AliPayment对象列表
     */
    public static List<AliPayment> parsCMBCsv(String filePath) {
        // 创建用于存储解析结果的列表
        List<AliPayment> payments = new ArrayList<>();
        // 标识是否开始解析数据
        boolean startParsing = false;
        // 用于存储读取的每一行数据
        String line;
        // 标识是否已跳过第一行
        boolean firstLineSkipped = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // 逐行读取文件内容
            while ((line = reader.readLine()) != null) {
                // 如果还未跳过第一行
                if (!firstLineSkipped) {
                    // 设置已跳过第一行标识为true
                    firstLineSkipped = true;
                    // 跳过当前行，继续下一行读取
                    continue; 
                }

                // 判断是否到达数据开始位置
                if (line.trim().equals(ALIPAY_CSV_HEADER)) {
                    // 设置开始解析标识为true
                    startParsing = true;
                    // 跳过当前行，继续下一行读取
                    continue;
                }

                // 如果已开始解析数据
                if (startParsing) {
                    // 按制表符分割每行数据
                    String[] fields = line.split("\t");

                    // 创建支付宝支付对象
                    AliPayment payment = new AliPayment();
                    // 设置交易时间
                    payment.setTransactionTime(fields[0]);
                    // 设置交易分类
                    payment.setTransactionType(fields[1]);
                    // 设置交易对方
                    payment.setCounterparty(fields[2]);
                    // 设置对方账号
                    payment.setCounterpartyAccount(fields[3]);
                    // 设置商品说明
                    payment.setProductDescription(fields[4]);
                    // 设置收/支
                    payment.setInOut(fields[5]);
                    // 设置金额
                    payment.setAmount(fields[6]);
                    // 设置收/付款方式
                    payment.setPaymentMethod(fields[7]);
                    // 设置交易状态
                    payment.setTransactionStatus(fields[8]);
                    // 设置交易订单号
                    payment.setTransactionOrderId(fields[9]);
                    // 设置商户订单号
                    payment.setMerchantOrderId(fields[10]);
                    // 设置备注
                    payment.setNote(fields[11]);

                    // 将解析出的支付对象添加到结果列表中
                    payments.add(payment);
                }
            }
        } catch (IOException e) {
            // 记录招商银行CSV文件解析异常
            log.error("解析招商银行CSV文件异常", e);
        }

        // 返回解析结果
        return payments;
    }


    /**
     * 生成一个带有指定数据的CSV文件。如果文件已存在，会追加数据并避免重复添加标题行
     * <p>
     * 该方法用于创建或向已存在的CSV文件中追加数据，确保不会重复添加标题行
     * </p>
     *
     * @param filePath   将创建或追加CSV文件的文件路径
     * @param addNewLine 确定在每行数据后是否添加新行
     * @param header     CSV文件的标题行
     * @param data       要写入CSV文件的一行数据
     */
    public static void generateCsvFile(String filePath, boolean addNewLine, String header, String data) {
        // 创建文件对象
        File file = new File(filePath);
        // 检查文件是否已存在
        boolean fileExists = file.exists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // 如果文件不存在或为空，则写入标题行
            if (!fileExists || file.length() == 0) {
                // 写入标题行
                writer.write(header);
                // 如果需要添加新行
                if (addNewLine) {
                    // 写入换行符
                    writer.newLine();
                }
            }

            // 写入第一行数据
            writer.write(data);
            // 如果需要添加新行
            if (addNewLine) {
                // 写入换行符
                writer.newLine();
            }

            // 记录信息
            log.info("数据已成功追加到CSV文件：{}", filePath);
        } catch (IOException e) {
            // 如果发生异常，则记录错误信息
            log.error("生成CSV文件时出错：{}", e.getMessage(), e);
        }
    }

    /**
     * 将指定文本追加写入到指定文件中
     * <p>
     * 该方法用于向指定文件中追加文本内容，支持控制是否添加换行符
     * </p>
     *
     * @param text       要写入文件的文本内容
     * @param filePath   文件路径，包括文件名和扩展名
     * @param addNewLine 是否在文本行末尾添加换行符
     */
    public static void writeTextToFile(String text, String filePath, boolean addNewLine) {
        // 启用追加模式
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            // 如果需要添加换行符
            if (addNewLine) {
                // 写入文本并添加换行符
                writer.write(text);
                // 使用newLine方法更规范地添加换行
                writer.newLine();
            } else {
                // 写入文本但不添加换行符
                writer.write(text);
            }

            // 记录信息
            log.info("文本成功追加到文件: {}", filePath);
        } catch (IOException e) {
            // 如果发生异常，则记录错误信息
            log.error("写入文件时出现错误: {}", e.getMessage(), e);
        }
    }

}
