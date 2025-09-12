package org.example.order.worker.kafka.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 통합테스트 전용 ORDER_API 리스너
 * - payload.id를 직접 파싱해 traceId/orderId MDC에 세팅 (SpEL/파라미터명 의존 제거)
 * - 운영에선 @Correlate로 처리하지만, 테스트에선 환경 영향을 배제하기 위해 수동 세팅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestOrderApiListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderApiMessageFacade facade;

    @KafkaListener(
            topics = "#{@orderApiTopic}",
            groupId = "order-order-api",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            final String json = record.value();
            final JsonNode root = MAPPER.readTree(json);
            final String id = root.path("id").asText(null);

            if (id != null) {
                MDC.put("orderId", id);
                MDC.put("traceId", id);
            }

            log.debug("[TEST] ORDER_API received: {}", json);

            facade.requestApi(json);
        } catch (Exception e) {
            log.error("[TEST] ORDER_API listener error", e);
        } finally {
            try {
                ack.acknowledge();
            } finally {
                MDC.clear();
            }
        }
    }
}
