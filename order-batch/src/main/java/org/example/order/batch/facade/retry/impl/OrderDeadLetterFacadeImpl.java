package org.example.order.batch.facade.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.facade.retry.OrderDeadLetterFacade;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadLetterFacadeImpl implements OrderDeadLetterFacade {

    private final OrderDeadLetterService orderDeadLetterService;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaTopicProperties kafkaTopicProperties;

    private static final String DEAD_LETTER_GROUP_ID = "group-order-dead-letter";
    private static final String CLIENT_SUFFIX = "dlt-client";

    @Override
    public void retry() {
        final String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ);

        try (Consumer<String, String> consumer = consumerFactory.createConsumer(DEAD_LETTER_GROUP_ID, CLIENT_SUFFIX)) {
            List<PartitionInfo> partitionsInfo = consumer.partitionsFor(topic);

            if (partitionsInfo == null || partitionsInfo.isEmpty()) {
                log.warn("DLQ topic has no partitions: {}", topic);

                return;
            }

            TopicPartition partition = new TopicPartition(topic, partitionsInfo.get(0).partition());
            Set<TopicPartition> partitions = Collections.singleton(partition);
            consumer.assign(partitions);

            Map<TopicPartition, OffsetAndMetadata> committedOffsets = consumer.committed(partitions);

            if (committedOffsets.get(partition) == null) {
                consumer.seekToBeginning(partitions);
            } else {
                consumer.seek(partition, committedOffsets.get(partition).offset());
            }

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1500));

            if (records.count() == 0) {
                log.debug("DLQ empty");

                return;
            }

            ConsumerRecord<String, String> record = records.records(topic).get(0);

            log.info("DLQ record offset={}, key={}, headers={}", record.offset(), record.key(), record.headers());

            try {
                orderDeadLetterService.retry(record.value());

                consumer.commitSync();

                log.info("DLQ commit offset={}", record.offset());
            } catch (Exception e) {
                log.error("dead-letter retry failed at offset={}", record.offset(), e);
            }
        } catch (Exception e) {
            log.error("dead-letter facade error", e);

            throw new CommonException(BatchExceptionCode.POLLING_FAILED);
        }
    }
}
