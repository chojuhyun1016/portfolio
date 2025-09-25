package org.example.order.common.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * (Deprecated) Producer 인터셉터
 * - PostProcessor 기반 구조가 메인입니다.
 * - 하위 호환을 위해 남겨두되 신규 코드에선 사용하지 마세요.
 */
@Deprecated
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

            return record;
        } catch (Throwable ignore) {
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
