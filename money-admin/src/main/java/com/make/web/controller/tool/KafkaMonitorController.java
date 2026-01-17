package com.make.web.controller.tool;

import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.system.domain.KafkaConsumerInfo;
import com.make.system.domain.KafkaTopicInfo;
import com.make.system.service.IKafkaMonitorService;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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
}
