package com.make.system.service;

import com.make.system.domain.KafkaConsumerInfo;
import com.make.system.domain.KafkaTopicInfo;

import java.util.List;
import java.util.Map;

public interface IKafkaMonitorService {
    List<KafkaTopicInfo> getTopics();
    List<String> getConsumerGroups();
    List<KafkaConsumerInfo> getConsumerGroupDetails(List<String> groupIds);
}
