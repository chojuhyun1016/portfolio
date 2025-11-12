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
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * OrderDeadLetterFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ에서 한 번 폴링하여 들어온 레코드를 모두 처리(재전송/폐기 판단) 후 종료.
 * 개선/수정
 * - 모든 파티션 assign + 파티션별 안전 커밋.
 * - 비어있을 때 INFO 로그로 노출(운영 가시성).
 * - consumer 레이어에서 JSON -> DeadLetter<?> 역직렬화(별도 @Qualifier 팩토리).
 * 추가 보강
 * - 파티션별 마지막 처리 오프셋(+1) 누적 추적 후, 정상/예외 종료 직전 최종 commitSync 한 번 더 수행.
 * - close(Duration) 타임아웃을 명시해 안정적인 종료 보장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadLetterFacadeImpl implements OrderDeadLetterFacade {

    private final OrderDeadLetterService orderDeadLetterService;

    @Qualifier("deadLetterConsumerFactory")
    private final ConsumerFactory<String, DeadLetter<?>> deadLetterConsumerFactory;

    private final KafkaTopicProperties kafkaTopicProperties;

    private static final String DEAD_LETTER_GROUP_ID = "group-order-dead-letter";
    private static final String CLIENT_SUFFIX = "dlt-client";
    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    @Override
    public void retry() {
        final String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ);

        Consumer<String, DeadLetter<?>> consumer = null;
        final Map<TopicPartition, OffsetAndMetadata> lastProcessedToCommit = new HashMap<>();

        try {
            consumer = deadLetterConsumerFactory.createConsumer(DEAD_LETTER_GROUP_ID, CLIENT_SUFFIX);

            List<PartitionInfo> infos = consumer.partitionsFor(topic);

            if (infos == null || infos.isEmpty()) {
                log.info("DLQ topic has no partitions: {}", topic);
                return;
            }

            // 모든 파티션 assign
            List<TopicPartition> partitions = new ArrayList<>();

            for (PartitionInfo pi : infos) {
                partitions.add(new TopicPartition(topic, pi.partition()));
            }

            consumer.assign(partitions);

            // 커밋된 오프셋 기준으로 seek (없으면 earliest)
            Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(new HashSet<>(partitions));

            for (TopicPartition tp : partitions) {
                OffsetAndMetadata meta = committed != null ? committed.get(tp) : null;

                if (meta == null) {
                    consumer.seekToBeginning(Collections.singleton(tp));
                } else {
                    consumer.seek(tp, meta.offset());
                }
            }

            // 한 번만 폴링해서 있는 레코드 처리
            ConsumerRecords<String, DeadLetter<?>> records = consumer.poll(Duration.ofSeconds(2));

            if (records == null || records.isEmpty()) {
                log.info("DLQ empty (no records polled)");

                return;
            }

            int processed = 0;

            // 파티션별로 모아서 커밋
            for (TopicPartition tp : records.partitions()) {
                List<ConsumerRecord<String, DeadLetter<?>>> list = records.records(tp);
                long lastOffset = -1L;

                for (ConsumerRecord<String, DeadLetter<?>> r : list) {
                    try {
                        processOne(consumer, r);
                        processed++;
                        lastOffset = r.offset();
                    } catch (Exception e) {
                        log.error("dead-letter retry failed at tp={}, offset={}", tp, r.offset(), e);
                    }
                }

                // 해당 파티션에서 처리한 마지막 오프셋 + 1 로 커밋
                if (lastOffset >= 0) {
                    OffsetAndMetadata oam = new OffsetAndMetadata(lastOffset + 1);
                    lastProcessedToCommit.put(tp, oam);
                    consumer.commitSync(Collections.singletonMap(tp, oam));

                    log.info("DLQ commit tp={}, lastOffsetCommitted={}", tp, lastOffset + 1);
                }
            }

            log.info("DLQ processed count={}", processed);

        } catch (Exception e) {
            log.error("dead-letter facade error", e);
            throw new CommonException(BatchExceptionCode.POLLING_FAILED);
        } finally {
            // 지금까지 누적된 마지막 오프셋(+1)을 한 번 더 동기 커밋(실패해도 로깅만)
            if (consumer != null && !lastProcessedToCommit.isEmpty()) {
                try {
                    consumer.commitSync(lastProcessedToCommit);

                    log.info("DLQ final commitSync done for {} partition(s).", lastProcessedToCommit.size());
                } catch (Exception ex) {
                    log.warn("DLQ final commitSync failed (ignored): {}", ex.toString());
                }
            }

            // 타임아웃 있는 종료로 네트워크/코디네이터 정리 기회 제공
            if (consumer != null) {
                try {
                    consumer.close(Duration.ofSeconds(5));
                } catch (Exception ex) {
                    log.warn("DLQ consumer close failed (ignored): {}", ex.toString());
                }
            }
        }
    }

    protected void processOne(Consumer<String, DeadLetter<?>> consumer,
                              ConsumerRecord<String, DeadLetter<?>> record) {
        Map<String, String> headers = extractHeaders(record.headers());
        normalizeRetryCount(headers);

        final DeadLetter<?> dlq = record.value();
        final MessageOrderType type = resolveTypeSafely(dlq.type());

        String orderId = resolveOrderId(headers);

        log.info("DLQ record tp={}-{}, offset={}, key={}, type={}, orderId={}, headers={}",
                record.topic(), record.partition(), record.offset(), record.key(), type, orderId, headers);

        switch (type) {
            case ORDER_LOCAL -> orderDeadLetterService.retryLocal(dlq, headers);
            case ORDER_API -> orderDeadLetterService.retryApi(dlq, headers);
            case ORDER_CRUD -> orderDeadLetterService.retryCrud(dlq, headers);
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }

    private static MessageOrderType resolveTypeSafely(Object rawType) {
        if (rawType instanceof MessageOrderType mt) {
            return mt;
        }

        if (rawType instanceof String s) {
            String norm = s.trim();

            if (norm.isEmpty()) {
                throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
            }

            try {
                return MessageOrderType.valueOf(norm.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
            }
        }

        throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
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
     * - 없거나 null/blank/음수/숫자아님 -> "0"
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

    private static String resolveOrderId(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "-";
        }

        String[] keys = new String[]{"orderId", "x-order-id", "X-Order-Id", "order-id"};

        for (String k : keys) {
            String v = headers.get(k);

            if (v != null && !v.isBlank()) {
                return v;
            }
        }

        return "-";
    }
}
