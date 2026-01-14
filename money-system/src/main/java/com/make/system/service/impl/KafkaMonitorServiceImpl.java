package com.make.system.service.impl;

import com.make.system.domain.KafkaConsumerInfo;
import com.make.system.domain.KafkaTopicInfo;
import com.make.system.service.IKafkaMonitorService;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class KafkaMonitorServiceImpl implements IKafkaMonitorService {

    private static final Logger log = LoggerFactory.getLogger(KafkaMonitorServiceImpl.class);

    @Resource
    private KafkaAdmin kafkaAdmin;

    @Resource
    private ConsumerFactory<String, String> consumerFactory;

    @Override
    public List<KafkaTopicInfo> getTopics() {
        List<KafkaTopicInfo> result = new ArrayList<>();
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties());
             org.apache.kafka.clients.consumer.Consumer<String, String> consumer = consumerFactory.createConsumer()) {

            ListTopicsResult topicsResult = admin.listTopics();
            Set<String> names = topicsResult.names().get();
            Map<String, TopicDescription> descriptions = admin.describeTopics(names).all().get();

            // Collect all partitions to query log end offsets
            List<TopicPartition> allPartitions = new ArrayList<>();
            for (TopicDescription desc : descriptions.values()) {
                desc.partitions().forEach(p -> allPartitions.add(new TopicPartition(desc.name(), p.partition())));
            }

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(allPartitions);

            for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
                TopicDescription desc = entry.getValue();
                KafkaTopicInfo info = new KafkaTopicInfo();
                info.setName(desc.name());
                info.setPartitionCount(desc.partitions().size());
                if (!desc.partitions().isEmpty()) {
                    info.setReplicationFactor(desc.partitions().get(0).replicas().size());
                }

                // Calculate total messages (Sum of Log End Offsets)
                // Note: This approximates "total produced" since retention policies delete old messages.
                // It represents "total current messages + deleted messages" effectively the high watermark sum.
                long totalMessages = 0;
                for (org.apache.kafka.common.TopicPartitionInfo p : desc.partitions()) {
                    TopicPartition tp = new TopicPartition(desc.name(), p.partition());
                    totalMessages += endOffsets.getOrDefault(tp, 0L);
                }
                info.setTotalMessageCount(totalMessages);

                result.add(info);
            }
        } catch (Exception e) {
            log.error("Failed to get topics", e);
        }
        return result;
    }

    @Override
    public List<String> getConsumerGroups() {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            return admin.listConsumerGroups().all().get().stream()
                    .map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list consumer groups", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<KafkaConsumerInfo> getConsumerGroupDetails(List<String> groupIds) {
        List<KafkaConsumerInfo> result = new ArrayList<>();
        if (groupIds == null || groupIds.isEmpty()) return result;

        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties());
             org.apache.kafka.clients.consumer.Consumer<String, String> consumer = consumerFactory.createConsumer()) {

            // 1. Describe Groups (State, Members)
            Map<String, ConsumerGroupDescription> groupDescriptions = admin.describeConsumerGroups(groupIds).all().get();

            for (String groupId : groupIds) {
                KafkaConsumerInfo groupInfo = new KafkaConsumerInfo();
                groupInfo.setGroupId(groupId);
                groupInfo.setPartitions(new ArrayList<>());

                ConsumerGroupDescription desc = groupDescriptions.get(groupId);
                if (desc != null) {
                    groupInfo.setState(desc.state().toString());
                    groupInfo.setCoordinator(desc.coordinator().host() + ":" + desc.coordinator().port());
                }

                // 2. Get Committed Offsets
                ListConsumerGroupOffsetsOptions options = new ListConsumerGroupOffsetsOptions();
                Map<TopicPartition, OffsetAndMetadata> committedOffsets = admin.listConsumerGroupOffsets(groupId, options)
                        .partitionsToOffsetAndMetadata().get();

                if (committedOffsets.isEmpty()) {
                    result.add(groupInfo);
                    continue;
                }

                // 3. Get Log End Offsets
                // Need to query end offsets for these partitions
                Set<TopicPartition> partitions = committedOffsets.keySet();
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

                long groupTotalLag = 0;

                // 4. Match and Calculate Lag
                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committedOffsets.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    OffsetAndMetadata offsetMeta = entry.getValue();
                    if (offsetMeta == null) continue;

                    KafkaConsumerInfo.PartitionInfo partInfo = new KafkaConsumerInfo.PartitionInfo();
                    partInfo.setTopic(tp.topic());
                    partInfo.setPartition(tp.partition());
                    partInfo.setCurrentOffset(offsetMeta.offset());

                    Long logEnd = endOffsets.getOrDefault(tp, 0L);
                    partInfo.setLogEndOffset(logEnd);

                    long lag = Math.max(0, logEnd - offsetMeta.offset());
                    partInfo.setLag(lag);
                    groupTotalLag += lag;

                    // Find member info if active
                    if (desc != null) {
                        desc.members().stream()
                                .filter(m -> m.assignment().topicPartitions().contains(tp))
                                .findFirst()
                                .ifPresent(m -> {
                                    partInfo.setMemberId(m.consumerId());
                                    partInfo.setClientId(m.clientId());
                                    partInfo.setHost(m.host());
                                });
                    }

                    groupInfo.getPartitions().add(partInfo);
                }

                groupInfo.setTotalLag(groupTotalLag);
                result.add(groupInfo);
            }

        } catch (Exception e) {
            log.error("Failed to get consumer details", e);
        }
        return result;
    }
}
