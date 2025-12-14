package com.make.quartz.task;

import com.make.quartz.service.IRealTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 获取固定时间的定时任务调度测试
 *
 * @author ruoyi
 */
@Component("fixedTimeTask")
public class FixedTimeTask {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(FixedTimeTask.class);


    @Resource
    private IRealTimeService realTimeService;


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

    /**
     * 获取新股申购股票
     *
     * @throws Exception
     */
    public void refreshNewStockInformation() {
        long startTime = System.currentTimeMillis();
        try {
            realTimeService.refreshNewStockInformation();
        } catch (Exception e) {
            log.error("执行查询异常:", e);
        }
        long endTime = System.currentTimeMillis();
        log.info("获取新股申购股票任务,耗时{}ms", endTime - startTime);
    }


    /**
     * 更新存款金额
     *
     * @throws Exception
     */
    public void updateDepositAmount() {
        long startTime = System.currentTimeMillis();
        try {
            realTimeService.updateDepositAmount();
            realTimeService.updateICBCDepositAmount(1L, 7L);
        } catch (Exception e) {
            log.error("存款金额更新异常:", e);
        }
        long endTime = System.currentTimeMillis();
        log.info("存款金额更新任务,耗时{}ms", endTime - startTime);
    }
}
