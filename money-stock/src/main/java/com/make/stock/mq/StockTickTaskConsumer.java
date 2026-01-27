package com.make.stock.mq;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.make.common.constant.KafkaTopics;
import com.make.common.utils.DateUtils;
import com.make.stock.domain.StockTick;
import com.make.stock.domain.dto.StockTickTaskDTO;
import com.make.stock.mapper.StockTickMapper;
import com.make.stock.util.KlineDataFetcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 股票逐笔成交任务消费者
 * 独立消费，负责获取并存储逐笔成交明细
 */
@Component
public class StockTickTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockTickTaskConsumer.class);

    @Resource
    private StockTickMapper stockTickMapper;

    @KafkaListener(topics = KafkaTopics.TOPIC_STOCK_TICK_TASK, groupId = "stock-tick-group", concurrency = "3")
    public void processTickTask(ConsumerRecord<String, String> record) {
        String msg = record.value();
        try {
            StockTickTaskDTO task = JSON.parseObject(msg, StockTickTaskDTO.class);
            if (task == null) return;

            long start = System.currentTimeMillis();

            // 1. Fetch Data
            JSONArray ticksJson = KlineDataFetcher.fetchStockTicks(task.getStockCode(), task.getMarket());
            if (ticksJson == null || ticksJson.isEmpty()) {
                return;
            }

            // 2. Prepare Date Context
            String todayStr = DateUtils.getDate(); // yyyy-MM-dd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat dateOnlySdk = new SimpleDateFormat("yyyy-MM-dd");
            Date today = dateOnlySdk.parse(todayStr); // Midnight

            // 3. Get Max Tick Count to filter
            Long maxTick = stockTickMapper.selectMaxTickCount(task.getStockCode(), today);
            long maxTickVal = (maxTick != null) ? maxTick : -1;

            // 4. Parse and Filter
            List<StockTick> toInsert = new ArrayList<>();
            Date now = DateUtils.getNowDate();

            for (int i = 0; i < ticksJson.size(); i++) {
                JSONObject item = ticksJson.getJSONObject(i);
                Long tickCount = item.getLong("tickCount");

                if (tickCount != null && tickCount > maxTickVal) {
                    StockTick tick = new StockTick();
                    tick.setStockCode(task.getStockCode());
                    tick.setTradeDate(today);

                    String timeStr = item.getString("time");
                    if (timeStr != null) {
                        try {
                            // timeStr is usually HH:mm:ss
                            tick.setTradeTime(sdf.parse(todayStr + " " + timeStr));
                        } catch (ParseException e) {
                            // ignore
                        }
                    }

                    tick.setPrice(item.getBigDecimal("price"));
                    tick.setVolume(item.getLong("volume"));
                    tick.setSideCode(item.getLong("sideCode"));
                    tick.setTickCount(tickCount);
                    tick.setAvgVol(item.getBigDecimal("avgVol"));
                    tick.setIsBigMoney(0); // Default
                    tick.setCreateTime(now);
                    tick.setUpdateTime(now);

                    toInsert.add(tick);
                }
            }

            // 5. Batch Insert
            if (!toInsert.isEmpty()) {
                stockTickMapper.insertStockTickBatch(toInsert);
                log.info("[TickConsumer] Inserted {} ticks for {}. TraceId={}, Cost={}ms",
                        toInsert.size(), task.getStockCode(), task.getTraceId(), (System.currentTimeMillis() - start));
            } else {
                log.debug("[TickConsumer] No new ticks for {}. TraceId={}", task.getStockCode(), task.getTraceId());
            }

        } catch (Exception e) {
            log.error("[TickConsumer] Failed to process task: {}", msg, e);
        }
    }
}
