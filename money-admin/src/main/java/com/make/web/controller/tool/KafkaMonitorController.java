package com.make.web.controller.tool;

import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.system.domain.KafkaConsumerInfo;
import com.make.system.domain.KafkaTopicInfo;
import com.make.system.service.IKafkaMonitorService;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import com.make.stock.service.scheduled.impl.StockWatchProcessor;
import com.make.stock.mq.StockConsumerLifecycleManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Kafka Monitor Controller
 */
@RestController
@RequestMapping("/tool/kafka")
public class KafkaMonitorController extends BaseController {

    @Resource
    private IKafkaMonitorService kafkaMonitorService;

    @Resource
    private StockTaskQueueService stockTaskQueueService;

    @Resource
    private StockWatchProcessor stockWatchProcessor;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PreAuthorize("@ss.hasPermi('tool:kafka:list')")
    @GetMapping("/topics")
    public AjaxResult listTopics() {
        List<KafkaTopicInfo> list = kafkaMonitorService.getTopics();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:list')")
    @GetMapping("/consumers")
    public AjaxResult listConsumers() {
        List<String> list = kafkaMonitorService.getConsumerGroups();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:list')")
    @PostMapping("/consumer/details")
    public AjaxResult getConsumerDetails(@RequestBody List<String> groupIds) {
        List<KafkaConsumerInfo> list = kafkaMonitorService.getConsumerGroupDetails(groupIds);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:remove')")
    @DeleteMapping("/topic/{topicName}")
    public AjaxResult deleteTopic(@PathVariable String topicName) {
        return toAjax(kafkaMonitorService.deleteTopic(topicName));
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:remove')")
    @DeleteMapping("/topic/{topicName}/messages")
    public AjaxResult deleteTopicMessages(@PathVariable String topicName) {
        return toAjax(kafkaMonitorService.deleteTopicMessages(topicName));
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:list')")
    @GetMapping("/topic/{topicName}/messages")
    public AjaxResult getTopicMessages(@PathVariable String topicName, @RequestParam(defaultValue = "10") int count) {
        return AjaxResult.success(kafkaMonitorService.getTopicMessages(topicName, count));
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:remove')")
    @PostMapping("/consumer/reset-offset")
    public AjaxResult resetConsumerOffset(@RequestParam String groupId, @RequestParam String topic) {
        return toAjax(kafkaMonitorService.resetConsumerGroupOffset(groupId, topic));
    }

    @PreAuthorize("@ss.hasPermi('tool:kafka:remove')")
    @PostMapping("/redis/clear-stock-status")
    public AjaxResult clearStockStatus(@RequestParam(required = false) String stockCode, @RequestParam(required = false) String traceId) {
        if (stockCode == null && traceId == null) {
            // Dangerous: Clear all? StockTaskQueueService needs a method for this.
            // For now, support only specific deletion or implement a 'clearAll' in service.
            // Since the user asked to "Clean Backlog", and the screenshot showed 1.9M items,
            // we need a way to clear the ZSet index.
            // Let's invoke a new method in StockTaskQueueService.
             stockTaskQueueService.clearAllStatuses(); // Need to implement this
             return AjaxResult.success("Initiated clear of all statuses");
        }
        stockTaskQueueService.deleteStatus(stockCode, traceId);
        return AjaxResult.success();
    }

    /**
     * 强制重置消费流程：
     * 1. 暂停所有节点消费者 (Pub/Sub)
     * 2. 重置 Kafka Offset 到最新 (Skip Backlog)
     * 3. 清空 Redis 任务状态 (Reset Status)
     * 4. 恢复所有节点消费者 (Pub/Sub)
     * 5. 立即触发新一轮任务 (Reproduce)
     */
    @PreAuthorize("@ss.hasPermi('tool:kafka:remove')")
    @PostMapping("/force-reset-reproduce")
    public AjaxResult forceResetAndReproduce() {
        try {
            // 1. Pause Consumers
            stringRedisTemplate.convertAndSend(StockConsumerLifecycleManager.CHANNEL, StockConsumerLifecycleManager.CMD_PAUSE);
            // Wait for propagation (heuristic)
            TimeUnit.SECONDS.sleep(2);

            // 2. Reset Offset (money-stock-group, stock-refresh)
            // Note: Currently hardcoded topics. Should be configurable or passed param.
            // Using logic from resetConsumerOffset but hardcoded for safety in this specific flow.
            kafkaMonitorService.resetConsumerGroupOffset("money-stock-group", "stock-refresh");

            // 3. Clear Redis Status
            stockTaskQueueService.clearAllStatuses();

            // 4. Resume Consumers
            stringRedisTemplate.convertAndSend(StockConsumerLifecycleManager.CHANNEL, StockConsumerLifecycleManager.CMD_RESUME);
            TimeUnit.SECONDS.sleep(1);

            // 5. Trigger Immediate Batch
            stockWatchProcessor.triggerImmediateBatch();

            return AjaxResult.success("Reset and Reproduce initiated successfully.");
        } catch (Exception e) {
            return AjaxResult.error("Failed to force reset: " + e.getMessage());
        }
    }
}
