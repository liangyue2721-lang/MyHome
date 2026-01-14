package com.make.quartz.task;

import com.make.common.constant.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * 获取实时数据的定时任务调度测试
 *
 * @author ruoyi
 */
@Component("realTimeTask")
public class RealTimeTask {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(RealTimeTask.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 更新美股实时行情数据。
     */
    public void updateWatchStockUs() {
        log.info("触发任务：更新美股实时行情数据 [TOPIC_WATCH_STOCK_US]");
        kafkaTemplate.send(KafkaTopics.TOPIC_WATCH_STOCK_US, "trigger");
    }

    /**
     * 查询今天是否有上市的股票
     */
    public void queryListingStatusColumn() {
        log.info("触发任务：查询今天是否有上市的股票 [TOPIC_QUERY_LISTING]");
        kafkaTemplate.send(KafkaTopics.TOPIC_QUERY_LISTING, "trigger");
    }
}
