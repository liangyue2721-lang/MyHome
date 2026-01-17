package com.make.system.service;

import com.make.system.domain.KafkaConsumerInfo;
import com.make.system.domain.KafkaTopicInfo;

import java.util.List;
import java.util.Map;

public interface IKafkaMonitorService {
    List<KafkaTopicInfo> getTopics();
    List<String> getConsumerGroups();
    List<KafkaConsumerInfo> getConsumerGroupDetails(List<String> groupIds);

    /**
     * 删除Topic
     */
    boolean deleteTopic(String topicName);

    /**
     * 清空Topic消息 (删除至当前LogEndOffset)
     */
    boolean deleteTopicMessages(String topicName);

    /**
     * 获取Topic消息详情 (采样最近N条)
     */
    List<Map<String, Object>> getTopicMessages(String topicName, int count);

    /**
     * 重置消费组 Offset 到最新 (跳过积压)
     */
    boolean resetConsumerGroupOffset(String groupId, String topic);
}
