package com.make.finance.domain.dto;


import lombok.Data;

@Data
public class MailReceiver {
    /**
     * 邮件服务器ip地址
     */
    private String mailServerHost;
    /**
     * 端口
     */
    private String mailServerPort;
    /**
     * 接收邮件地址
     */
    private String receiveEmailAddress;

    /**
     * 接收邮件地址
     */
    private String receiveEmailPassword;
}
