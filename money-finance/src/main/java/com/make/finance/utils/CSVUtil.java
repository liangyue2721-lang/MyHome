package com.make.finance.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.make.finance.domain.dto.AliPayment;
import com.make.finance.domain.dto.WeChatTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            // 初始化行计数器数组，用于记录当前读取的行数
            final int[] rowCount = {0}; // 行计数器

            // 使用EasyExcel读取文件
            EasyExcel.read(file, WeChatTransaction.class, new AnalysisEventListener<WeChatTransaction>() {
                /**
                 * 每解析一行数据时调用该方法
                 *
                 * @param record  当前行解析出的数据对象
                 * @param context 解析上下文信息
                 */
                @Override
                public void invoke(WeChatTransaction record, AnalysisContext context) {
                    // 每次调用时行计数器加1
                    rowCount[0]++; 

                    // 从第17行开始获取数据（跳过表头等无关信息）
                    if (rowCount[0] > 16) {
                        // 将解析出的记录添加到结果列表中
                        records.add(record); 
                    }
                }

                /**
                 * 所有数据解析完成后调用该方法
                 *
                 * @param context 解析上下文信息
                 */
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 处理完成后的逻辑
                    log.info("解析完成，共{}条记录", records.size());
                    // 这里可以添加将数据保存到数据库或其他操作
                }
            }).sheet().doRead();
        } catch (Exception e) {
            // 记录解析异常日志
            log.error("微信流水解析异常：{}", e.getMessage());
        }

        // 返回解析结果
        return records;
    }


    /**
     * 解析支付宝流水的CSV文件，并将每行数据转换为Payment对象列表
     * <p>
     * 通过传统方式逐行读取支付宝导出的CSV文件，并将其转换为AliPayment对象列表
     * </p>
     *
     * @param filePath CSV文件路径
     * @return 包含解析后Payment对象的列表
     */
    public static List<AliPayment> parseAlipayCsv(String filePath) {
        // 创建用于存储解析结果的列表
        List<AliPayment> payments = new ArrayList<>();
        // 标识是否开始解析数据
        boolean startParsing = false;
        // 用于存储读取的每一行数据
        String line;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // 逐行读取文件内容
            while ((line = reader.readLine()) != null) {
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
            log.error("解析支付宝CSV文件异常", e);
        }

        // 返回解析结果
        return payments;
    }

    /**
     * 使用EasyExcel解析支付宝支付CSV文件
     * <p>
     * 通过EasyExcel框架读取支付宝支付导出的CSV文件，并将其转换为AliPayment对象列表
     * </p>
     *
     * @param file 支付宝支付CSV文件
     * @return 包含解析后支付宝支付记录的列表
     */
    public static List<AliPayment> easyExcelParseAlipayCsv(File file) {
        // 创建用于存储解析结果的列表
        List<AliPayment> records = new ArrayList<>();
        try {
            // 初始化行计数器数组，用于记录当前读取的行数
            final int[] rowCount = {0}; // 行计数器

            // 使用EasyExcel读取文件
            EasyExcel.read(file, AliPayment.class, new AnalysisEventListener<AliPayment>() {
                /**
                 * 每解析一行数据时调用该方法
                 *
                 * @param record  当前行解析出的数据对象
                 * @param context 解析上下文信息
                 */
                @Override
                public void invoke(AliPayment record, AnalysisContext context) {
                    // 每次调用时行计数器加1
                    rowCount[0]++; 

                    // 从第23行开始获取数据（跳过表头等无关信息）
                    if (rowCount[0] > 22) {
                        // 将解析出的记录添加到结果列表中
                        records.add(record); 
                    }
                }

                /**
                 * 所有数据解析完成后调用该方法
                 *
                 * @param context 解析上下文信息
                 */
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 处理完成后的逻辑
                    System.out.println("解析完成，共" + records.size() + "条记录");
                    // 这里可以添加将数据保存到数据库或其他操作
                }
            }).sheet().doRead();
        } catch (Exception e) {
            // 记录解析异常日志（注：此处日志信息有误，应为支付宝流水解析异常）
            log.error("微信流水解析异常：{}", e.getMessage());
        }

        // 返回解析结果
        return records;
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