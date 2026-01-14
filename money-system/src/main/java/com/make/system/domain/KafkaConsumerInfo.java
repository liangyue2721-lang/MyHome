package com.make.system.domain;

import lombok.Data;

import java.util.List;

@Data
public class KafkaConsumerInfo {
    private String groupId;
    private String state;
    private String coordinator;
    private List<PartitionInfo> partitions;
    private long totalLag;

    @Data
    public static class PartitionInfo {
        private String topic;
        private int partition;
        private long currentOffset;
        private long logEndOffset;
        private long lag;
        private String memberId;
        private String clientId;
        private String host;
    }
}
