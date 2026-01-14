package com.make.finance.util.email;

import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.finance.domain.dto.EmailEntity;
import com.make.finance.domain.dto.MailReceiver;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 邮件服务工具类
 * <p>
 * 提供邮件接收功能，优先使用IMAPS/SMTPS协议，失败后可切换到POP3S/SMTPS协议。
 * 主要用于接收建设银行信用卡交易通知邮件并解析其中的交易信息
 * </p>
 */
public class EmailReader {

    // ==== 常量配置（根据实际情况替换） ====
    /**
     * IMAP邮件服务器主机地址
     * <p>
     * 用于连接邮件服务器接收邮件
     * </p>
     */
    private static final String IMAP_HOST = "imap.qq.com";
    
    /**
     * IMAP邮件服务器端口号
     * <p>
     * IMAP协议的标准SSL端口号为993
     * </p>
     */
    private static final String IMAP_PORT = "993";

    /**
     * 用户邮箱地址
     * <p>
     * 用于接收邮件的邮箱地址
     * </p>
     */
    private static final String USER_EMAIL = "845220936@qq.com";
    
    /**
     * 用户邮箱授权码
     * <p>
     * 用于登录邮箱的授权码，而非邮箱密码
     * </p>
     */
    private static final String USER_AUTH_CODE = "bnlqitmvrwjgbdhg";

    /**
     * 获取建设银行信用卡交易短信验证码及相关信息
     * <p>
     * 通过IMAP协议连接邮箱服务器，接收指定发件人的邮件，
     * 并解析邮件内容获取信用卡交易信息
     * </p>
     *
     * @return 建设银行信用卡交易信息列表
     * @throws Exception 邮件接收或解析过程中可能抛出的异常
     */
    public static List<CCBCreditCardTransactionEmail> getSmsCode() throws Exception {
        // 创建邮件接收工具类实例
        ReceiveEmailUtil util = new ReceiveEmailUtil();
        // 创建邮件接收配置对象
        MailReceiver qqReceive = new MailReceiver();
        // 设置邮件服务器主机地址
        qqReceive.setMailServerHost(IMAP_HOST);
        // 设置邮件服务器端口号
        qqReceive.setMailServerPort(IMAP_PORT);
        // 设置接收邮件的邮箱地址
        qqReceive.setReceiveEmailAddress(USER_EMAIL);//邮箱地址
        // 设置邮箱授权码（IMAP授权码）
        qqReceive.setReceiveEmailPassword(USER_AUTH_CODE);//imap授权码
        // 其它属性按需要填充
        // 创建IMAP ID信息映射表
        HashMap<String, String> iam = new HashMap<>();
        // 接收指定发件人的邮件
        List<EmailEntity> emailEntities = util.receiveEmail(qqReceive, null, null, iam, "15511644036@163.com");
        // 创建用于存储所有信用卡交易信息的列表
        List<CCBCreditCardTransactionEmail> ccbCreditCardTransactionsAll = new ArrayList<>();
        // 判断是否有接收到邮件
        if (CollectionUtils.isNotEmpty(emailEntities)) {
            // 遍历所有接收到的邮件
            for (EmailEntity emailEntity : emailEntities) {
                // 获取邮件内容
                String content = emailEntity.getContent();
                // 解析邮件内容获取信用卡交易信息
                List<CCBCreditCardTransactionEmail> ccbCreditCardTransactions = CCBCreditCardTransactionEmail.parseTransactions(content);
                // 将解析出的交易信息添加到总列表中
                ccbCreditCardTransactionsAll.addAll(ccbCreditCardTransactions);
            }
        }
        // 返回所有信用卡交易信息列表
        return ccbCreditCardTransactionsAll;
    }
}




