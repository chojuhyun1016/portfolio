package org.example.order.client.kafka.interceptor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.example.order.client.kafka.interceptor.MdcHeaderNames.*;

/**
 * MdcHeadersProducerInterceptor
 * ------------------------------------------------------------------------
 * 목적
 * - Kafka 메시지 발행 시, 현재 스레드 MDC의 traceId/orderId를 Kafka 헤더에 자동 전파.
 * - 기존 동일 키 헤더가 있으면 제거 후 재주입(중복 방지).
 * <p>
 * 장점
 * - Spring Kafka 버전에 의존하지 않음(ProducerRecordPostProcessor 없이 동작).
 * - 비즈니스 코드 변경 없이 전역 적용(ProducerFactory 설정에 interceptor 등록).
 */
public class MdcHeadersProducerInterceptor implements ProducerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        try {
            var headers = record.headers();

            String traceId = MDC.get(MDC_TRACE_ID);

            if (traceId != null && !traceId.isBlank()) {
                headers.remove(TRACE_ID);
                headers.add(new RecordHeader(TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8)));
            }

            String orderId = MDC.get(MDC_ORDER_ID);

            if (orderId != null && !orderId.isBlank()) {
                headers.remove(ORDER_ID);
                headers.add(new RecordHeader(ORDER_ID, orderId.getBytes(StandardCharsets.UTF_8)));
            }

            return record;
        } catch (Throwable ignore) {
            // 인터셉터에서 예외 던지면 발행 자체가 막히므로 안전하게 무시
            return record;
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no-op
    }
}
