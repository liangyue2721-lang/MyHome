package com.make.system.domain;

import lombok.Data;

@Data
public class KafkaTopicInfo {
    private String name;
    private int partitionCount;
    private int replicationFactor;
    private long totalMessageCount; // Approximate
}
