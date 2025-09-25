package org.example.order.client.kafka.interceptor;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;

import static org.example.order.client.kafka.interceptor.MdcHeaderNames.*;

/**
 * MdcRecordInterceptor
 * ------------------------------------------------------------------------
 * 목적
 * - Kafka 컨슈머 수신 시, 헤더(traceId, orderId)를 읽어 MDC에 복원한다.
 * - traceId 헤더가 없고 key가 있으면 traceId를 key로 보정한다.
 */
public class MdcRecordInterceptor<V> implements RecordInterceptor<String, V> {

    @Override
    public ConsumerRecord<String, V> intercept(ConsumerRecord<String, V> record,
                                               Consumer<String, V> consumer) {

        putHeaderToMdc(record, TRACE_ID, MDC_TRACE_ID);
        putHeaderToMdc(record, ORDER_ID, MDC_ORDER_ID);

        // traceId 미존재 시 key로 보강
        if (isBlank(MDC.get(MDC_TRACE_ID)) && !isBlank(record.key())) {
            MDC.put(MDC_TRACE_ID, record.key());
        }

        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<String, V> record,
                            Consumer<String, V> consumer) {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_ORDER_ID);
    }

    // ⚠ static 제거: 제네릭 V를 안전하게 사용하기 위함
    private void putHeaderToMdc(ConsumerRecord<String, V> record, String headerName, String mdcKey) {
        Header h = record.headers().lastHeader(headerName);

        if (h != null && h.value() != null && h.value().length > 0) {
            String v = new String(h.value(), StandardCharsets.UTF_8);

            if (!isBlank(v)) {
                MDC.put(mdcKey, v);
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
