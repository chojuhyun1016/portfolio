package org.example.order.client.kafka.interceptor;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * MdcRecordInterceptor
 * ------------------------------------------------------------------------
 * 목적
 * - Kafka 컨슈머 수신 시, 헤더("traceId")를 읽어 MDC["traceId"]에 복원.
 * - Kafka Key를 MDC["orderId"]로 저장하여 비즈 키 기반 로그 상관관계 강화.
 * - (보강) 헤더 traceId가 없고 key가 있으면 traceId도 key로 보정.
 */
public class MdcRecordInterceptor<V> implements RecordInterceptor<String, V> {

    public static final String HEADER_NAME = "traceId";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_ORDER_ID = "orderId";

    @Override
    public ConsumerRecord<String, V> intercept(ConsumerRecord<String, V> record,
                                               Consumer<String, V> consumer) {

        // traceId 헤더 → MDC
        Header h = record.headers().lastHeader(HEADER_NAME);
        if (h != null) {
            String traceId = new String(h.value(), StandardCharsets.UTF_8);
            if (traceId != null && !traceId.isBlank()) {
                MDC.put(MDC_TRACE_ID, traceId);
            }
        }

        // orderId(MDC) = Kafka key
        String key = record.key();
        if (key != null && !key.isBlank()) {
            MDC.put(MDC_ORDER_ID, key);

            if (MDC.get(MDC_TRACE_ID) == null || MDC.get(MDC_TRACE_ID).isBlank()) {
                MDC.put(MDC_TRACE_ID, key);
            }
        }

        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<String, V> record,
                            Consumer<String, V> consumer) {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_ORDER_ID);
    }
}
