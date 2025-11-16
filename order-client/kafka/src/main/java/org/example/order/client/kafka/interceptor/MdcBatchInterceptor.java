package org.example.order.client.kafka.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

/**
 * MdcBatchInterceptor
 * ------------------------------------------------------------------------
 * 역할 (배치 리스너 전용)
 * - 첫 레코드의 Kafka 헤더(traceId, orderId)를 MDC에 심어서 로그 상관관계 유지
 * - 각 레코드별로 value == null 인 경우
 * - 헤더 덤프
 * - springDeserializerException* 헤더를 DeserializationException 으로 복원
 * → 예외 메시지 + 깨진 payload 일부를 WARN 로그로 남김
 */
@Slf4j
public class MdcBatchInterceptor<K, V> implements BatchInterceptor<K, V> {

    private static final String HEADER_TRACE_ID = "traceId";
    private static final String HEADER_ORDER_ID = "orderId";

    @Override
    public ConsumerRecords<K, V> intercept(ConsumerRecords<K, V> records, Consumer<K, V> consumer) {
        if (records == null || records.isEmpty()) {
            return records;
        }

        // 1) 첫 레코드 기준으로 MDC 복원 (동일 배치 내에서는 traceId / orderId 동일하다고 가정)
        ConsumerRecord<K, V> first = records.iterator().next();
        restoreMdcFromHeaders(first.headers());

        // 2) 각 레코드별 상태 로그
        for (ConsumerRecord<K, V> record : records) {
            if (record == null) {
                continue;
            }

            if (record.value() == null) {
                log.warn("[MdcBatchInterceptor] Deserialized value is NULL " +
                                "(topic={}, partition={}, offset={}, keyClass={}, key={})",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key() == null ? "null" : record.key().getClass().getName(),
                        record.key());

                logHeaders(record.headers());
                logDeserializationException(record.headers());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[MdcBatchInterceptor] Deserialized record OK " +
                                    "(topic={}, partition={}, offset={}, keyClass={}, valueClass={})",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            record.key() == null ? "null" : record.key().getClass().getName(),
                            record.value().getClass().getName());
                }
            }
        }

        return records;
    }

    /* ======================= 공통 유틸 ======================= */

    private void restoreMdcFromHeaders(Headers headers) {
        String traceId = getHeaderAsString(headers, HEADER_TRACE_ID);
        String orderId = getHeaderAsString(headers, HEADER_ORDER_ID);

        if (traceId != null) {
            MDC.put("traceId", traceId);
        }

        if (orderId != null) {
            MDC.put("orderId", orderId);
        }
    }

    private String getHeaderAsString(Headers headers, String name) {
        Header header = headers.lastHeader(name);

        if (header == null) {
            return null;
        }

        if (header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    /**
     * 모든 헤더 키/길이를 찍어서 ErrorHandlingDeserializer 관련 헤더가 있는지 확인용
     */
    private void logHeaders(Headers headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (Header h : headers) {
            sb.append(h.key())
                    .append("=byte[")
                    .append(h.value() == null ? "null" : h.value().length)
                    .append("], ");
        }

        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("]");

        log.warn("[MdcBatchInterceptor] Headers: {}", sb);
    }

    /**
     * springDeserializerException* 헤더를 DeserializationException 으로 복원해서
     * - 예외 메시지
     * - 깨졌던 원본 데이터 일부
     * 를 로그로 남긴다.
     */
    private void logDeserializationException(Headers headers) {
        for (Header header : headers) {
            String key = header.key();

            if (key == null || !key.startsWith("springDeserializerException")) {
                continue;
            }

            byte[] value = header.value();

            if (value == null || value.length == 0) {
                log.warn("[MdcBatchInterceptor] Deserialization exception header '{}' has empty value", key);
                continue;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(value))) {
                Object obj = ois.readObject();

                if (obj instanceof DeserializationException ex) {
                    String msg = ex.getMessage();
                    byte[] data = ex.getData();
                    String dataSnippet = null;

                    if (data != null) {
                        String raw = new String(data, StandardCharsets.UTF_8);
                        dataSnippet = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
                    }

                    log.warn("[MdcBatchInterceptor] Detected DeserializationException from header '{}': message={}",
                            key, msg);

                    if (dataSnippet != null) {
                        log.warn("[MdcBatchInterceptor] Offending payload snippet from header '{}': {}",
                                key, dataSnippet);
                    } else {
                        log.warn("[MdcBatchInterceptor] No offending payload data present in exception header '{}'", key);
                    }
                } else {
                    log.warn("[MdcBatchInterceptor] Header '{}' is not DeserializationException but {}",
                            key, obj == null ? "null" : obj.getClass().getName());
                }
            } catch (Exception e) {
                log.warn("[MdcBatchInterceptor] Failed to deserialize deserialization-exception header '{}': {}",
                        key, e.toString());
            }
        }
    }
}
