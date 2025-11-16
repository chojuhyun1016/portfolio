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
 * - local-order-local 단건 수신
 * - 로컬(마스터)에서 들어온 LocalOrderMessage를 API 토픽으로 전달
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageListenerImpl implements OrderLocalMessageListener {

    private final OrderLocalMessageFacade facade;

    private static final String DEFAULT_TYPE =
            "org.example.order.contract.order.messaging.event.OrderLocalMessage";

    @Override
    @KafkaListener(
            topics = "#{@orderLocalTopic}",
            groupId = "group-order-local",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "2",
            properties = {
                    "spring.json.value.default.type=" + DEFAULT_TYPE
            }
    )
    @Correlate(
            key = "#record.value() != null && #record.value().id() != null ? #record.value().id() : T(java.util.UUID).randomUUID().toString()",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void orderLocal(ConsumerRecord<String, OrderLocalMessage> record,
                           Acknowledgment acknowledgment) {
        try {
            OrderLocalMessage message = record.value();

            log.info("LOCAL - order-local record received: {}", message);

            if (message == null) {
                log.warn("LOCAL - order-local record value is null. key={}", record.key());

                return;
            }

            OrderLocalConsumerDto dto = OrderLocalConsumerDto.from(message);

            ConsumerEnvelope<OrderLocalConsumerDto> envelope =
                    ConsumerEnvelope.fromRecord(record, dto);

            facade.sendOrderApiTopic(envelope);
        } catch (Exception e) {
            log.error("error : order-local", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
