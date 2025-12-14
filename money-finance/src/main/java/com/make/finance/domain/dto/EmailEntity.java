package com.make.finance.domain.dto;

import lombok.Data;

import java.util.List;

//邮件实体类
@Data
public class EmailEntity {
    // 发件日期
    private String sentDate;
    // 收件日期
    private String receiveDate;
    // 邮件主题
    private String subject;
    // 收件人
    private List<String> toName;
    // 收件邮箱地址
    private List<String> toAddress;
    // 发送人
    private String fromAddress;
    // 发件邮箱地址
    private String fromName;
    // 抄送人
    private List<String> ccName;
    // 抄送邮箱地址
    private List<String> ccAddress;
    // 邮件正文
    private String content;
    // 回复人
    private List<String> replyName;
    // 回复邮箱地址
    private List<String> replyAddress;
}
