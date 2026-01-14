package com.make.web.controller.tool;

import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.system.domain.KafkaConsumerInfo;
import com.make.system.domain.KafkaTopicInfo;
import com.make.system.service.IKafkaMonitorService;
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
}
