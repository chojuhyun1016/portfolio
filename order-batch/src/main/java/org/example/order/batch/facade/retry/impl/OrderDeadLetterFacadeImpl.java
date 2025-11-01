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

/**
 * OrderDeadLetterFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ에서 **단건**을 폴링하여 재처리(기존 알고리즘 동일).
 * <p>
 * 개선/수정 사항
 * - Iterable 에서는 get(index)가 불가 → iterator()로 첫 레코드 안전 획득.
 * - Topic 문자열 기반 records(...) 대신 TopicPartition 기반 records(...) 사용.
 * - 커밋 로직/시킹 로직 기존 유지, NPE 방지 보강.
 */
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

            // 단일 파티션 선택(기존 구현 유지 — 필요 시 라운드로빈 확장 가능)
            TopicPartition partition = new TopicPartition(topic, partitionsInfo.get(0).partition());
            Set<TopicPartition> partitions = Collections.singleton(partition);
            consumer.assign(partitions);

            Map<TopicPartition, OffsetAndMetadata> committedOffsets = consumer.committed(partitions);

            if (committedOffsets == null || committedOffsets.get(partition) == null) {
                consumer.seekToBeginning(partitions);
            } else {
                consumer.seek(partition, committedOffsets.get(partition).offset());
            }

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1500));

            if (records == null || records.isEmpty()) {
                log.debug("DLQ empty");

                return;
            }

            // TopicPartition 기반 Iterable 얻기
            Iterable<ConsumerRecord<String, String>> iterable = records.records(partition);

            if (iterable == null) {
                log.debug("DLQ iterable is null");

                return;
            }

            Iterator<ConsumerRecord<String, String>> it = iterable.iterator();

            if (!it.hasNext()) {
                log.debug("DLQ has no record in the partition {}", partition);

                return;
            }

            ConsumerRecord<String, String> record = it.next();

            log.info("DLQ record offset={}, key={}, headers={}", record.offset(), record.key(), record.headers());

            try {
                // 원문 value(JSON) 를 서비스에 위임
                orderDeadLetterService.retry(record.value());

                // 현재 포지션 커밋(기존 동작 유지)
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
