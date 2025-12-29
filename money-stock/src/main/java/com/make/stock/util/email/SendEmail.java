package com.make.stock.util.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


/**
 * 发送邮件通知
 *
 * @author Devil
 */
public final class SendEmail {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(SendEmail.class);

    public static Session createSession() {
        Properties pros = new Properties();
        pros.put("mail.smtp.host", "smtp.163.com");
        pros.put("mail.smtp.port", "25");
        pros.put("mail.smtp.auth", "true");
//        pros.put("mail.smtp.starttls.enable", "true");

        //创建Session
        Session session = Session.getInstance(pros, new Authenticator() {
            ;
            String userName = "lyp0028nxyf@163.com";
            String password = "JWNKSOFGHRZRUNFB";

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });
        session.setDebug(true);
        return session;
    }


    public static void notification(String text, String subject) {
        //1.创建Session
        try {
            Session session = createSession();
            System.out.println(session);

            //2.创建邮件对象
            MimeMessage message = new MimeMessage(session);
            //设置邮件主题
            message.setSubject(subject);
            //设置邮件内容
            message.setText(text);
            //设置发件人
            message.setFrom(new InternetAddress("lyp0028nxyf@163.com"));
            //设置收件人
            message.setRecipient(Message.RecipientType.TO, new InternetAddress("lyp0028nxyf@163.com"));
            //3.发送邮件
            Transport.send(message);
        } catch (AddressException e) {
            log.error(e.getMessage());
        } catch (MessagingException e) {
            log.error("发送邮件出错{}", e.getMessage());
        }

    }

    public static void notification(String msgText, String subject, String from) {
        //1.创建Session
        try {
            Session session = createSession();
            System.out.println(session);

            //2.创建邮件对象
            MimeMessage message = new MimeMessage(session);
            //设置邮件主题
            message.setSubject(subject);
            //设置邮件内容
            message.setText(msgText);
            //设置发件人
            message.setFrom(new InternetAddress("lyp0028nxyf@163.com"));
            //设置收件人
            message.setRecipient(Message.RecipientType.TO, new InternetAddress("lyp0028nxyf@163.com"));
            //3.发送邮件
            Transport.send(message);
        } catch (AddressException e) {
            log.error(e.getMessage());
        } catch (MessagingException e) {
            log.error("发送邮件出错{}", e.getMessage());
        }

    }
}
