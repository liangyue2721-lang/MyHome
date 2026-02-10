package com.make.finance.service.scheduled.finance.impl;

import com.make.common.utils.DateUtils;
import com.make.common.utils.SecurityUtils;
import com.make.finance.domain.CbcCreditCardTransaction;
import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.finance.service.ICbcCreditCardTransactionService;
import com.make.finance.service.scheduled.finance.CreditCardService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 信用卡服务实现类
 * <p>负责处理建设银行信用卡交易记录的保存和解析</p>
 * <p>包含完整的数据验证、转换和默认值处理机制</p>
 * 
 * @author Ruoyi
 * @version 1.1
 * @since 2023-09-01
 */
@Service
public class CreditCardServiceImpl implements CreditCardService {

    private static final Logger log = LoggerFactory.getLogger(CreditCardServiceImpl.class);

    /**
     * 日期格式化器，用于解析交易日期
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Resource
    private ICbcCreditCardTransactionService cbcCreditCardTransactionService;

    /**
     * 保存建设银行信用卡交易记录
     * <p>将从邮件解析出的交易记录转换为数据库实体并批量保存</p>
     *
     * @param emailList 从邮件解析出的交易记录列表，不能为空
     * @throws RuntimeException 当数据转换或保存过程中发生错误时抛出
     *
     * <p><b>处理流程:</b>
     * <ol>
     *   <li>验证输入参数有效性</li>
     *   <li>将邮件DTO对象转换为数据库实体对象</li>
     *   <li>验证转换后数据的完整性</li>
     *   <li>批量插入到数据库中</li>
     *   <li>记录操作日志</li>
     * </ol>
     *
     * <p><b>注意事项:</b>
     * <ul>
     *   <li>会自动填充用户ID和创建时间</li>
     *   <li>日期格式必须为 yyyy-MM-dd</li>
     *   <li>金额字段会自动转换为BigDecimal类型</li>
     *   <li>空列表会被直接忽略，不执行任何操作</li>
     * </ul>
     */
    @Override
    public void saveCCBCreditCardTransaction(List<CCBCreditCardTransactionEmail> emailList) {
        // 1. 参数校验
        if (CollectionUtils.isEmpty(emailList)) {
            log.warn("【CreditCardService】接收到空的交易记录列表，跳过保存操作");
            return;
        }

        try {
            // 2. 数据转换：将邮件DTO转换为数据库实体
            List<CbcCreditCardTransaction> transactionList = convertEmailListToTransactionList(emailList);

            if (CollectionUtils.isEmpty(transactionList)) {
                log.warn("【CreditCardService】转换后的交易记录列表为空，跳过保存操作");
                return;
            }

            // 3. 批量保存到数据库
            int savedCount = cbcCreditCardTransactionService.batchInsertCbcCreditCardTransaction(transactionList);

            // 4. 记录操作结果
            log.info("【CreditCardService】成功保存信用卡交易记录，请求数量: {}, 实际保存数量: {}",
                    emailList.size(), savedCount);

        } catch (Exception e) {
            log.error("【CreditCardService】保存信用卡交易记录失败，交易记录数量: {}",
                    emailList != null ? emailList.size() : 0, e);
            throw new RuntimeException("保存信用卡交易记录失败", e);
        }
    }

    /**
     * 将邮件交易记录列表转换为数据库实体列表
     * <p>执行必要的数据类型转换和格式化操作</p>
     *
     * @param emailList 邮件交易记录列表
     * @return 数据库实体列表
     * @throws RuntimeException 当日期解析失败时抛出
     */
    private List<CbcCreditCardTransaction> convertEmailListToTransactionList(List<CCBCreditCardTransactionEmail> emailList) {
        return emailList.stream()
                .map(this::convertEmailToTransaction)
                .collect(Collectors.toList());
    }

    /**
     * 将单个邮件交易记录转换为数据库实体
     * <p>处理日期解析、用户信息填充等转换逻辑</p>
     *
     * @param email 邮件交易记录
     * @return 数据库实体
     * @throws RuntimeException 当日期解析失败时抛出
     */
    private CbcCreditCardTransaction convertEmailToTransaction(CCBCreditCardTransactionEmail email) {
        CbcCreditCardTransaction transaction = new CbcCreditCardTransaction();

        try {
            // 转换交易日期
            if (email.getTradeDate() != null && !email.getTradeDate().trim().isEmpty()) {
                transaction.setTradeDate(DATE_FORMAT.parse(email.getTradeDate().trim()));
            }

            // 转换入账日期
            if (email.getPostDate() != null && !email.getPostDate().trim().isEmpty()) {
                transaction.setPostDate(DATE_FORMAT.parse(email.getPostDate().trim()));
            }

        } catch (ParseException e) {
            log.error("【CreditCardService】日期解析失败，交易日期: {}, 入账日期: {}",
                    email.getTradeDate(), email.getPostDate(), e);
            throw new RuntimeException("日期格式解析失败: " + e.getMessage(), e);
        }

        // 设置基本信息，处理空值情况
        transaction.setCardLast4(defaultIfEmpty(email.getCardLast4(), "0000"));
        transaction.setDescription(defaultIfEmpty(email.getDescription(), "未知交易"));
        transaction.setTransAmount(convertAmount(email.getTransAmount()));
        transaction.setSettleAmount(convertAmount(email.getTransAmount()));
        
        // 设置默认备注信息
        transaction.setRemark(buildDefaultRemark(email));

        // 填充用户信息
        transaction.setUserId(SecurityUtils.getUserId());

        // 设置创建时间为当前时间
        Date now = DateUtils.getNowDate();
        transaction.setCreateTime(now);
        transaction.setUpdateTime(now);

        return transaction;
    }

    /**
     * 转换金额字符串为BigDecimal
     * <p>处理可能为空或格式异常的金额字段</p>
     *
     * @param amountStr 金额字符串，可能包含币种信息
     * @return BigDecimal金额，解析失败时返回null
     */
    /**
     * 转换金额字符串为BigDecimal
     * <p>处理可能为空或格式异常的金额字段</p>
     *
     * @param amountStr 金额字符串，可能包含币种信息
     * @return BigDecimal金额，解析失败时返回null
     */
    private BigDecimal convertAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 如果包含斜杠，提取金额部分（假设格式为 "CNY/1500.00"）
            String cleanAmount = amountStr;
            if (amountStr.contains("/")) {
                String[] parts = amountStr.split("/");
                if (parts.length >= 2) {
                    cleanAmount = parts[1].trim();
                }
            }

            // 清理可能的货币符号和其他非数字字符
            cleanAmount = cleanAmount.replaceAll("[^0-9.-]", "");

            if (!cleanAmount.isEmpty()) {
                BigDecimal amount = new BigDecimal(cleanAmount);
                // 确保金额为正数（数据库中可能存储为负数表示支出）
                return amount.abs();
            }
        } catch (NumberFormatException e) {
            log.warn("【CreditCardService】金额转换失败: {}", amountStr, e);
        }

        return null;
    }

    /**
     * 如果字符串为空则返回默认值
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 处理后的值
     */
    private String defaultIfEmpty(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    /**
     * 构建默认备注信息
     * <p>基于交易信息生成有意义的备注</p>
     *
     * @param email 邮件交易记录
     * @return 备注信息
     */
    private String buildDefaultRemark(CCBCreditCardTransactionEmail email) {
        StringBuilder remark = new StringBuilder();
        remark.append("信用卡交易");
        
        if (email.getCardLast4() != null && !email.getCardLast4().trim().isEmpty()) {
            remark.append("-尾号").append(email.getCardLast4().trim());
        }
        
        if (email.getTradeDate() != null && !email.getTradeDate().trim().isEmpty()) {
            remark.append("-").append(email.getTradeDate().trim());
        }
        
        return remark.toString();
    }

    /**
     * 验证交易记录的必填字段
     * <p>确保数据完整性，避免插入无效数据</p>
     *
     * @param transactions 交易记录列表
     * @throws IllegalArgumentException 当必填字段缺失时抛出
     */
    private void validateTransactions(List<CbcCreditCardTransaction> transactions) {
        for (int i = 0; i < transactions.size(); i++) {
            CbcCreditCardTransaction transaction = transactions.get(i);
            
            // 验证必填字段
            if (transaction.getUserId() == null) {
                throw new IllegalArgumentException(
                    String.format("第%d条记录用户ID不能为空", i + 1));
            }
            
            if (transaction.getCardLast4() == null || transaction.getCardLast4().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("第%d条记录卡号后四位不能为空", i + 1));
            }
            
            if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("第%d条记录交易描述不能为空", i + 1));
            }
            
            if (transaction.getTransAmount() == null) {
                log.warn("【CreditCardService】第{}条记录交易金额为空，使用默认值0", i + 1);
                transaction.setTransAmount(BigDecimal.ZERO);
            }
            
            // 验证日期字段
            if (transaction.getTradeDate() == null) {
                log.warn("【CreditCardService】第{}条记录交易日期为空，使用当前日期", i + 1);
                transaction.setTradeDate(new Date());
            }
        }
    }
}