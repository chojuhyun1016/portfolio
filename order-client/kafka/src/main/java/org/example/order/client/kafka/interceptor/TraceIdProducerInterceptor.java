package org.example.order.client.kafka.interceptor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * TraceIdProducerInterceptor
 * ------------------------------------------------------------------------
 * 목적
 * - Kafka 메시지 발행 시, 현재 스레드의 MDC["traceId"]를 Kafka 헤더("traceId")로 자동 전파.
 * <p>
 * 배치 위치
 * - order-client 모듈 (카프카 클라이언트 공통)
 * <p>
 * 주의
 * - 비즈니스 코드 수정 없이 전역 적용 가능(ProducerFactory 설정에 interceptor 등록)
 */
public class TraceIdProducerInterceptor implements ProducerInterceptor<String, Object> {

    public static final String HEADER_NAME = "traceId";
    public static final String MDC_KEY = "traceId";

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {

        String traceId = MDC.get(MDC_KEY);

        if (traceId != null && !traceId.isBlank()) {
            record.headers().add(new RecordHeader(HEADER_NAME, traceId.getBytes(StandardCharsets.UTF_8)));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
