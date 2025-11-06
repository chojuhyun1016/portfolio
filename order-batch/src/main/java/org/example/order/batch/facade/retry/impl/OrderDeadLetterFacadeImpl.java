package org.example.order.batch.facade.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.facade.retry.OrderDeadLetterFacade;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.common.support.logging.Correlate;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * OrderDeadLetterFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ에서 폴링하여 레코드 개별 재처리(재전송/폐기 판단).
 * - 레코드 단위로 MDC 상관키를 AOP(@Correlate)로 설정.
 * <p>
 * 개선/수정 사항
 * - TopicPartition 기반 records(...) 사용(기존 유지).
 * - 레코드별 개별 커밋.
 * - Kafka 헤더 -> Map<String, String> 추출 후 서비스에 전달(재시도 카운트 헤더 반영).
 * - 메시지 타입 분류는 Facade에서 수행하고, Service의 타입별 메서드에 위임.
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
    private static final String RETRY_COUNT_HEADER = "x-retry-count";

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

            Iterable<ConsumerRecord<String, String>> iterable = records.records(partition);

            if (iterable == null) {
                log.debug("DLQ iterable is null");

                return;
            }

            int processed = 0;

            for (ConsumerRecord<String, String> record : iterable) {
                try {
                    processOne(consumer, record);
                    processed++;
                } catch (Exception e) {
                    log.error("dead-letter retry failed at offset={}", record.offset(), e);
                }
            }

            log.info("DLQ processed count={}", processed);

        } catch (Exception e) {
            log.error("dead-letter facade error", e);

            throw new CommonException(BatchExceptionCode.POLLING_FAILED);
        }
    }

    /**
     * 단일 레코드 처리
     * - @Correlate 로 MDC(orderId) 설정: 헤더 -> key 순서
     * - 처리 성공 시 개별 커밋
     * - 공통 정책: 재전송 카운트 헤더가 없거나 null/blank면 "0"으로 초기화
     * - Facade에서 메시지 타입 판별 후 Service의 타입별 메서드에 위임
     */
    @Correlate(
            paths = {
                    "#p1?.headers()?.get('orderId')",
                    "#p1?.headers()?.get('traceId')",
                    "#p1?.headers()?.get('X-Request-Id')",
                    "#p1?.headers()?.get('x-request-id')",
                    "#p1?.key()"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    protected void processOne(Consumer<String, String> consumer, ConsumerRecord<String, String> record) {
        Map<String, String> headers = extractHeaders(record.headers());

        normalizeRetryCount(headers);

        final MessageOrderType type = resolveType(record.value());

        log.info("DLQ record offset={}, key={}, type={}, headers={}", record.offset(), record.key(), type, headers);

        switch (type) {
            case ORDER_LOCAL -> orderDeadLetterService.retryLocal(record.value(), headers);
            case ORDER_API -> orderDeadLetterService.retryApi(record.value(), headers);
            case ORDER_CRUD -> orderDeadLetterService.retryCrud(record.value(), headers);
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }

        consumer.commitSync();

        log.info("DLQ commit offset={}", record.offset());
    }

    private static Map<String, String> extractHeaders(Headers hs) {
        Map<String, String> map = new LinkedHashMap<>();

        if (hs == null) {
            return map;
        }

        for (Header h : hs) {
            if (h == null || h.key() == null) {
                continue;
            }

            String val = (h.value() == null) ? null : new String(h.value(), StandardCharsets.UTF_8);
            map.put(h.key(), val);
        }

        return map;
    }

    /**
     * 재전송 카운트 헤더 보정
     * - 없거나 null/blank -> "0"
     * - 숫자 아님/음수 -> "0"
     */
    private static void normalizeRetryCount(Map<String, String> headers) {
        if (headers == null) {
            return;
        }

        String v = headers.get(RETRY_COUNT_HEADER);

        if (v == null || v.isBlank()) {
            headers.put(RETRY_COUNT_HEADER, "0");

            return;
        }

        try {
            int n = Integer.parseInt(v.trim());
            headers.put(RETRY_COUNT_HEADER, (n < 0) ? "0" : Integer.toString(n));
        } catch (NumberFormatException nfe) {
            headers.put(RETRY_COUNT_HEADER, "0");
        }
    }

    private static MessageOrderType resolveType(Object raw) {
        try {
            return ObjectMapperUtils.getFieldValueFromString(String.valueOf(raw), "type", MessageOrderType.class);
        } catch (Exception e) {
            throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }
}
