package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.common.support.logging.Correlate;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.worker.dto.consumer.OrderLocalConsumerDto;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.listener.order.OrderLocalMessageListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * OrderLocalMessageListenerImpl
 * - Local 메시지 단건 수신
 * - 레코드를 Envelope로 감싸 파사드에 위임
 * - 변환/검증/에러처리는 파사드에서 일관 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageListenerImpl implements OrderLocalMessageListener {

    private static final String DEFAULT_TYPE =
            "org.example.order.contract.order.messaging.event.OrderLocalMessage";

    private final OrderLocalMessageFacade facade;

    @Override
    @KafkaListener(
            topics = "#{@orderLocalTopic}",
            groupId = "group-order-local",
            concurrency = "2",
            properties = {
                    "spring.json.value.default.type=" + DEFAULT_TYPE
            }
    )
    @Correlate(
            paths = {
                    "#p0?.value()?.id",
                    "#p0?.value()?.orderId",
                    "#p0?.value()?.payload?.id",
                    "#p0?.key()",
                    "#p0?.headers()?.get('orderId')",
                    "#p0?.headers()?.get('traceId')",
                    "#p0?.headers()?.get('X-Request-Id')",
                    "#p0?.headers()?.get('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void orderLocal(ConsumerRecord<String, OrderLocalMessage> record, Acknowledgment acknowledgment) {
        log.info("LOCAL - order-local record received: {}", record.value());

        try {
            ConsumerEnvelope<OrderLocalConsumerDto> envelope =
                    ConsumerEnvelope.fromRecord(record, OrderLocalConsumerDto.from(record.value()));

            facade.sendOrderApiTopic(envelope);
        } catch (Exception e) {
            log.error("error : order-local", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
