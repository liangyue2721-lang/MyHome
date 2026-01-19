package com.make.quartz.task;

/*
import com.make.common.constant.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
*/

/**
 * 获取固定时间的定时任务调度测试
 * (已废弃：业务逻辑已迁移至 StockTriggerTask / FinanceTriggerTask)
 *
 * @author ruoyi
 */
// @Component("fixedTimeTask")
public class FixedTimeTask {

    /*
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(FixedTimeTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    public static String get(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            return response.toString();
        } else {
            return "GET request not worked";
        }
    }

    public void refreshNewStockInformation() {
        String traceId = UUID.randomUUID().toString();
        log.info("触发任务：获取新股申购股票 [TOPIC_NEW_STOCK_INFO] traceId={}", traceId);
        kafkaTemplate.send(KafkaTopics.TOPIC_NEW_STOCK_INFO, traceId, "trigger");
    }

    public void updateDepositAmount() {
        String traceId1 = UUID.randomUUID().toString();
        log.info("触发任务：更新存款金额 [TOPIC_DEPOSIT_UPDATE] traceId={}", traceId1);
        kafkaTemplate.send(KafkaTopics.TOPIC_DEPOSIT_UPDATE, traceId1, "trigger");

        String traceId2 = UUID.randomUUID().toString();
        log.info("触发任务：更新工商银行存款 [TOPIC_ICBC_DEPOSIT_UPDATE] traceId={}", traceId2);
        kafkaTemplate.send(KafkaTopics.TOPIC_ICBC_DEPOSIT_UPDATE, traceId2, "trigger");
    }
    */
}
