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
 * - Kafka 컨슈머 수신 시, 헤더("traceId")를 읽어 MDC["traceId"]에 복원하여 로그 추적 연결.
 * <p>
 * 배치 위치
 * - order-client 모듈 (카프카 클라이언트 공통)
 * <p>
 * 주의
 * - @Component로 자동 스캔하지 않고, 컨테이너 팩토리에서 명시적으로 setRecordInterceptor()로 주입.
 * - (Spring Kafka 3.x) RecordInterceptor의 추상 메서드는 Consumer 파라미터 포함 시그니처여서,
 * intercept(record, consumer) / afterRecord(record, consumer)를 구현해야 한다.
 */
public class MdcRecordInterceptor implements RecordInterceptor<String, String> {

    public static final String HEADER_NAME = "traceId";
    public static final String MDC_KEY = "traceId";

    @Override
    public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record,
                                                    Consumer<String, String> consumer) {

        Header h = record.headers().lastHeader(HEADER_NAME);

        if (h != null) {
            String traceId = new String(h.value(), StandardCharsets.UTF_8);

            if (traceId != null && !traceId.isBlank()) {
                MDC.put(MDC_KEY, traceId);
            }
        }

        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<String, String> record,
                            Consumer<String, String> consumer) {
        // 레코드 처리 이후, 현재 스레드 MDC 정리 (다른 레코드에 오염 방지)
        MDC.remove(MDC_KEY);
    }
}
