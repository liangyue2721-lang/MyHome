package com.make.stock.service.scheduled.impl;

import com.alibaba.fastjson2.JSON;
import com.make.common.constant.KafkaTopics;
import com.make.stock.domain.EtfData;
import com.make.stock.domain.dto.EtfRealtimeInfo;
import com.make.stock.service.IEtfDataService;
import com.make.stock.util.KlineDataFetcher;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ETF 任务处理器
 * <p>
 * 重构说明：
 * 采用 Kafka 生产-消费模型，实现动态负载均衡。
 * 1. submitTasks: 生产者，查询所有 ETF 并生产单条任务消息推送到 Kafka。
 * 2. processSingleTask: 消费者，监听 Topic 处理单个 ETF 数据更新。
 * </p>
 */
@Component
public class StockETFProcessor {

    private static final Logger log = LoggerFactory.getLogger(StockETFProcessor.class);

    @Resource
    private IEtfDataService etfDataService;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 提交所有 ETF 任务到 Kafka (Producer)
     * <p>
     * 逻辑说明：
     * 1. 查询所有需要更新的 ETF 数据。
     * 2. 遍历列表，将每条 ETF 封装成消息发送到 Kafka (TOPIC_ETF_TASK)。
     * 3. 消费者端将接收并处理这些任务，从而实现集群间的负载均衡。
     *
     * @param traceId 链路追踪ID
     */
    public void submitTasks(String traceId) {
        log.info("=====【ETF 任务提交】TraceId={} =====", traceId);

        // 1. 获取所有 ETF 数据
        List<EtfData> etfDataList = etfDataService.selectEtfDataList(null);
        if (CollectionUtils.isEmpty(etfDataList)) {
            log.warn("[ETF] TraceId={} 无数据,跳过提交", traceId);
            return;
        }

        log.info("[ETF-Producer] TraceId={} 待提交数量={}", traceId, etfDataList.size());

        // 2. 遍历并逐个发送到 Kafka
        for (EtfData etfData : etfDataList) {
            try {
                // 构建消息载荷
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", etfData.getId());
                payload.put("etfCode", etfData.getEtfCode());
                payload.put("stockApi", etfData.getStockApi());
                payload.put("traceId", traceId);

                String json = JSON.toJSONString(payload);
                // 发送消息，使用 etfCode 作为 Key 可保证分区有序性（虽然此处主要为了负载均衡，Key可随机或指定）
                kafkaTemplate.send(KafkaTopics.TOPIC_ETF_TASK, etfData.getEtfCode(), json);
            } catch (Exception e) {
                log.error("[ETF-Producer] 发送失败 TraceId={} Code={} Err={}", traceId, etfData.getEtfCode(), e.getMessage());
            }
        }

        log.info("=====【ETF 任务提交完成】TraceId={} =====", traceId);
    }

    /**
     * 处理单个 ETF 任务 (Consumer)
     * <p>
     * 逻辑说明：
     * 1. 解析 Kafka 消息中的 ETF 信息。
     * 2. 调用 API 获取实时行情。
     * 3. 更新数据库。
     *
     * @param message Kafka 消息内容
     */
    public void processSingleTask(String message) {
        try {
            // 1. 解析消息
            Map<String, Object> payload = JSON.parseObject(message, Map.class);
            String traceId = (String) payload.get("traceId");
            String etfCode = (String) payload.get("etfCode");
            String stockApi = (String) payload.get("stockApi");
            String id = payload.get("id") != null ? payload.get("id").toString() : null;

            log.debug("[ETF-Consumer] 开始处理 TraceId={} Code={}", traceId, etfCode);

            // 2. 获取实时行情
            EtfRealtimeInfo info = KlineDataFetcher.fetchEtfRealtimeInfo(stockApi);
            if (info != null) {
                // 3. 转换并更新数据库
                EtfData mapped = EtfData.etfRealtimeInfoToEtfData(info);
                mapped.setId(id);
                mapped.setEtfCode(etfCode);

                etfDataService.updateEtfData(mapped);
                log.debug("[ETF-Consumer] 更新成功 TraceId={} Code={}", traceId, etfCode);
            } else {
                log.warn("[ETF-Consumer] 无行情数据 TraceId={} Code={}", traceId, etfCode);
            }
        } catch (Exception e) {
            log.error("[ETF-Consumer] 处理异常 Msg={} Err={}", message, e);
        }
    }
}
