package org.example.order.common.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Producer 인터셉터
 * - MDC의 traceId/orderId를 Kafka 헤더에 주입한다.
 * - 기존 헤더가 있으면 덮어쓴다(정책 변경 원하면 if-null일 때만 주입하도록 수정 가능).
 */
public class MdcToHeaderProducerInterceptor implements ProducerInterceptor<Object, Object> {

    private static final String TRACE_ID = "traceId";
    private static final String ORDER_ID = "orderId";

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        try {
            var headers = record.headers();

            String traceId = MDC.get(TRACE_ID);

            if (traceId != null) {
                headers.remove(TRACE_ID);
                headers.add(TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8));
            }

            String orderId = MDC.get(ORDER_ID);

            if (orderId != null) {
                headers.remove(ORDER_ID);
                headers.add(ORDER_ID, orderId.getBytes(StandardCharsets.UTF_8));
            }

            // 기존 레코드 그대로 반환(헤더는 mutable)
            return record;
        } catch (Throwable ignore) {
            // 인터셉터에서 예외 던지면 발행 자체가 막히므로 안전하게 무시
            return record;
        }
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
