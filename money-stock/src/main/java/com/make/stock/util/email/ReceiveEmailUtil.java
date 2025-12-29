package com.make.stock.util.email;

import com.make.finance.domain.dto.EmailEntity;
import com.make.finance.domain.dto.MailReceiver;
import com.sun.mail.imap.IMAPStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

//读取qq邮箱工具类
public class ReceiveEmailUtil {
    private static final Logger log = LoggerFactory.getLogger(ReceiveEmailUtil.class);

    public List<EmailEntity> receiveEmail(MailReceiver mailReceiver, Date start, Date end,
                                          HashMap<String, String> iam) {
        // 准备连接服务器的会话信息
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.host", mailReceiver.getMailServerHost());
        props.setProperty("mail.imap.port", mailReceiver.getMailServerPort());
        props.setProperty("mail.imap.auth.login.disable", "true");
        props.setProperty("mail.imap.ssl.enable", "true");

        List<EmailEntity> res = null;
        Session session = null;
        // 获取收件箱
        IMAPStore store = null;
        Folder folder = null;
        Thread t = Thread.currentThread();
        ClassLoader ccl = t.getContextClassLoader();
        t.setContextClassLoader(Session.class.getClassLoader());
        try {
            session = Session.getInstance(props, new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    //发件人邮件用户名、授权码
                    return new PasswordAuthentication(mailReceiver.getReceiveEmailAddress(),
                            mailReceiver.getReceiveEmailPassword());
                }
            });
            store = (IMAPStore) session.getStore("imap");
            store.connect(mailReceiver.getMailServerHost(),
                    mailReceiver.getReceiveEmailAddress(), mailReceiver.getReceiveEmailPassword());
            //带上IMAP ID信息，由key和value组成，例如name，version，vendor，support-email等。
            store.id(iam);
            folder = store.getFolder("INBOX");
            // 以读写方式打开
            folder.open(Folder.READ_ONLY);
            res = new ArrayList<>();
            Message[] messages = getEmail(folder, start, end, res);
            Message[] messages2 = getEmailBySender(folder, "service@vip.ccb.com", start, end);
            detailEmail(messages, res);
            folder.close();
            store.close();
        } catch (MessagingException | IOException e) {
            log.error("邮件接收异常", e);
        } finally {
            t.setContextClassLoader(ccl);
        }
        return res;
    }

    /**
     * 删除指定发件人的邮件，并立即将其从服务器中清除（EXPUNGE）。
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 假设 store 已连接，folder = store.getFolder("INBOX")
     * folder.open(Folder.READ_WRITE);
     * int deletedCount = receiveEmailUtil.deleteEmailsBySender(folder,
     *     "service@vip.ccb.com", startDate, endDate);
     * System.out.println("已删除邮件数量：" + deletedCount);
     * }</pre>
     *
     * @param folder      已打开的 INBOX 文件夹，必须是 READ_WRITE 模式
     * @param fromAddress 发件人邮箱地址，例如 "alice@example.com"
     * @param start       可选的开始时间（包含），为 null 则不设下限
     * @param end         可选的结束时间（包含），为 null 则不设上限
     * @return 实际删除的邮件数
     * @throws MessagingException 当搜索或删除操作失败时抛出
     * @throws IOException        当底层 I/O 错误时抛出
     */
    public int deleteEmailsBySender(Folder folder,
                                    String fromAddress,
                                    Date start,
                                    Date end) throws MessagingException, IOException {
        // 1. 组合搜索条件：发件人 + 可选时间范围
        SearchTerm fromTerm = new FromStringTerm(fromAddress);
        SearchTerm dateTerm = null;
        if (start != null && end != null) {
            dateTerm = new AndTerm(
                    new ReceivedDateTerm(ComparisonTerm.GE, start),
                    new ReceivedDateTerm(ComparisonTerm.LE, end)
            );
        } else if (start != null) {
            dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, start);
        } else if (end != null) {
            dateTerm = new ReceivedDateTerm(ComparisonTerm.LE, end);
        }
        SearchTerm term = dateTerm == null ? fromTerm : new AndTerm(fromTerm, dateTerm);

        // 2. 在服务器端搜索符合条件的消息
        Message[] toDelete = folder.search(term);
        if (toDelete == null || toDelete.length == 0) {
            return 0;
        }

        // 3. 标记为 DELETED
        for (Message msg : toDelete) {
            msg.setFlag(Flags.Flag.DELETED, true);
        }

        // 4. 立即 expunge（关闭时生效）
        //    注意：关闭并重开会话后，此 folder 无法再用，需要重新打开
        folder.close(true);

        return toDelete.length;
    }

    /**
     * 接收邮件（只获取未读邮件 + 按发件人 + 按时间区间）
     *
     * @param mailReceiver 邮箱账号配置
     * @param start        开始时间，可 null
     * @param end          结束时间，可 null
     * @param iam          IMAP ID 信息
     * @param fromAddress  发件人邮箱，可 null
     * @return 邮件实体列表
     */
    public List<EmailEntity> receiveEmail(
            MailReceiver mailReceiver,
            Date start,
            Date end,
            HashMap<String, String> iam,
            String fromAddress) {

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.host", mailReceiver.getMailServerHost());
        props.setProperty("mail.imap.port", mailReceiver.getMailServerPort());
        props.setProperty("mail.imap.auth.login.disable", "true");
        props.setProperty("mail.imap.ssl.enable", "true");

        List<EmailEntity> res = new ArrayList<>();

        Session session;
        IMAPStore store = null;
        Folder folder = null;

        // 修复 JavaMail ClassLoader 问题
        Thread t = Thread.currentThread();
        ClassLoader ccl = t.getContextClassLoader();
        t.setContextClassLoader(Session.class.getClassLoader());

        try {
            session = Session.getInstance(props, new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            mailReceiver.getReceiveEmailAddress(),
                            mailReceiver.getReceiveEmailPassword()
                    );
                }
            });

            store = (IMAPStore) session.getStore("imap");
            store.connect(
                    mailReceiver.getMailServerHost(),
                    mailReceiver.getReceiveEmailAddress(),
                    mailReceiver.getReceiveEmailPassword()
            );

            store.id(iam);

            folder = store.getFolder("INBOX");

            // ======== ⚠ 必须读写模式，才能标记已读 =========
            folder.open(Folder.READ_WRITE);

            // 按未读 + 发件人 + 时间 过滤邮件
            Message[] messages = searchUnreadEmails(folder, fromAddress, start, end);

            // 解析邮件内容
            detailEmail(messages, res);

            // ========= 自动把所有邮件标记为已读 =========
            markAsRead(messages);

            folder.close(true);
            store.close();

        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        } finally {
            t.setContextClassLoader(ccl);
        }

        return res;
    }

    /**
     * 搜索未读 + 发件人 + 时间范围
     */
    private Message[] searchUnreadEmails(
            Folder folder,
            String fromAddress,
            Date start,
            Date end
    ) throws MessagingException {

        List<SearchTerm> terms = new ArrayList<>();

        // 未读邮件
        terms.add(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        if (fromAddress != null) {
            terms.add(new FromStringTerm(fromAddress));
        }

        if (start != null) {
            terms.add(new ReceivedDateTerm(ComparisonTerm.GE, start));
        }

        if (end != null) {
            terms.add(new ReceivedDateTerm(ComparisonTerm.LE, end));
        }

        SearchTerm finalTerm;
        if (terms.size() == 1) {
            finalTerm = terms.get(0);
        } else {
            finalTerm = new AndTerm(terms.toArray(new SearchTerm[0]));
        }

        return folder.search(finalTerm);
    }

    /**
     * 将邮件设为已读
     */
    private void markAsRead(Message[] messages) throws MessagingException {
        for (Message message : messages) {
            message.setFlag(Flags.Flag.SEEN, true);
        }
    }


    /**
     * 获取邮件列表
     *
     * @param folder
     * @param start
     * @param end
     * @return
     */
    private Message[] getEmail(Folder folder, Date start, Date end, List<EmailEntity> res) throws MessagingException {
        Message[] messages;
        int num = folder.getMessageCount();
        if ((start == null) && (end == null)) {
            // 获取最后一封邮件
//            int emailIndex = Math.max((num - 1), 1);
            messages = folder.getMessages(num, num);
        } else {
            int emailIndex = Math.max((num - 99), 1);
            Message[] allMessages = folder.getMessages(emailIndex, num);
            messages = Arrays.stream(allMessages).filter(m -> {
                try {
                    return betweenDate(m, start, end);
                } catch (MessagingException e) {
                    log.error("邮件日期过滤异常", e);
                }
                return false;
            }).toArray(Message[]::new);
        }
        return messages;
    }

    /**
     * 获取指定发件人、指定时间区间的邮件列表
     *
     * @param folder      已打开的 Folder（请确保它处于 Folder.READ_ONLY 或 Folder.READ_WRITE 状态）
     * @param fromAddress 发件人邮箱地址，格式如 "alice@example.com"
     * @param start       查询开始时间（包含），可为 null 表示不设下限
     * @param end         查询结束时间（包含），可为 null 表示不设上限
     * @return 满足条件的 Message 数组
     */
    private Message[] getEmailBySender(Folder folder,
                                       String fromAddress,
                                       Date start,
                                       Date end) throws MessagingException {

        // 构造发件人条件
        SearchTerm fromTerm = new FromStringTerm(fromAddress);

        // 构造时间范围条件
        SearchTerm dateTerm = null;
        if (start != null && end != null) {
            SearchTerm after = new ReceivedDateTerm(ComparisonTerm.GE, start);
            SearchTerm before = new ReceivedDateTerm(ComparisonTerm.LE, end);
            dateTerm = new AndTerm(after, before);
        } else if (start != null) {
            dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, start);
        } else if (end != null) {
            dateTerm = new ReceivedDateTerm(ComparisonTerm.LE, end);
        }

        // 组合所有非空条件
        SearchTerm combined;
        if (dateTerm != null) {
            combined = new AndTerm(fromTerm, dateTerm);
        } else {
            combined = fromTerm;
        }

        // 执行服务器端搜索
        Message[] found = folder.search(combined);

        // 如果只是想取最近 N 封，可以在这里截断
        // int N = 100;
        // if (found.length > N) {
        //     return Arrays.copyOfRange(found, found.length - N, found.length);
        // }

        return found;
    }

    /**
     * 筛选时间段内邮件
     *
     * @param message
     * @param start
     * @param end
     * @return
     */
    private boolean betweenDate(Message message, Date start, Date end) throws MessagingException {
        if (message.getReceivedDate() == null) {
//            log.error("receiveDate null {}", message.getSubject());
            return false;
        }
        return message.getReceivedDate().before(end) &&
                message.getReceivedDate().after(start) ||
                message.getReceivedDate().equals(start);
    }

    /**
     * 处理每个邮件具体信息
     *
     * @param messages
     * @param res
     */
    private void detailEmail(Message[] messages, List<EmailEntity> res) throws MessagingException, IOException {
        for (Message e : messages) {
            EmailEntity email = new EmailEntity();
            // 只保留第一个发件人
            if (e.getFrom() != null) {
                InternetAddress sender = (InternetAddress) e.getFrom()[0];
                email.setFromAddress(sender.getAddress());
                email.setFromName(sender.getPersonal());
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String receiveDate = e.getReceivedDate() == null ? "null" : sdf.format(e.getReceivedDate());
            email.setReceiveDate(receiveDate);
            String sendDate = e.getSentDate() == null ? "null" : sdf.format(e.getSentDate());
            email.setSentDate(sendDate);
            email.setSubject(e.getSubject());
            HashMap<String, List<String>> to = addressConvert(e.getRecipients(Message.RecipientType.TO));
            email.setToAddress(to.get("address"));
            email.setToName(to.get("name"));
            HashMap<String, List<String>> cc = addressConvert(e.getRecipients(Message.RecipientType.CC));
            email.setCcAddress(cc.get("address"));
            email.setCcName(cc.get("name"));
            HashMap<String, List<String>> re = addressConvert(e.getReplyTo());
            email.setReplyAddress(re.get("address"));
            email.setReplyName(re.get("name"));
            email.setContent(getBody(e));
            res.add(email);
        }
    }

    /**
     * 处理邮件地址和用户名
     *
     * @param original
     * @return
     */
    private HashMap<String, List<String>> addressConvert(Address[] original) {
        HashMap<String, List<String>> res = new HashMap<>();
        if (original == null) {
            res.put("name", null);
            res.put("address", null);
            return res;
        }
        List<String> name = new ArrayList<>();
        List<String> address = new ArrayList<>();
        Arrays.stream(original).forEach(t -> {
            InternetAddress ter = (InternetAddress) t;
            address.add(ter.getAddress());
            name.add(ter.getPersonal());
        });
        res.put("name", name);
        res.put("address", address);
        return res;
    }

    /**
     * 递归处理邮件正文
     *
     * @param part
     * @return
     */
    private String getBody(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/*")) {
            // Part是文本:
            return part.getContent().toString();
        }
        if (part.isMimeType("multipart/*")) {
            // Part是一个Multipart对象:
            Multipart multipart = (Multipart) part.getContent();
            // 循环解析每个子Part:
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String body = getBody(bodyPart);
                if (!body.isEmpty()) {
                    return body;
                }
            }
        }
        return "";
    }

    public Date getStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        return todayStart.getTime();
    }

    /**
     * 返回明天
     *
     * @param today
     * @return
     */
    public Date tomorrow(Date today) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        return calendar.getTime();
    }
}
