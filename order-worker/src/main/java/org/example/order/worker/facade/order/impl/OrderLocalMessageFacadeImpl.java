package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.springframework.stereotype.Component;

import org.example.order.common.support.logging.Correlate;

/**
 * OrderLocalMessageFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - 로컬 메시지 → API 메시지 변환/발행.
 * MDC 전략
 * - Kafka 헤더 traceId 복원 후, @Correlate 로 OrderLocalMessage.id 기반 traceId 덮어쓰기.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageFacadeImpl implements OrderLocalMessageFacade {

    private final KafkaProducerService kafkaProducerService;

    @Override
    @Correlate(
            key = "T(org.example.order.common.support.json.ObjectMapperUtils)" +
                    ".valueToObject(#record.value(), T(org.example.order.core.infra.messaging.order.message.OrderLocalMessage)).id",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void sendOrderApiTopic(ConsumerRecord<String, Object> record) {

        OrderLocalMessage message = null;

        try {
            message = ObjectMapperUtils.valueToObject(record.value(), OrderLocalMessage.class);

            log.debug("order-local record : {}", message);

            message.validation();

            kafkaProducerService.sendToOrderApi(OrderApiMessage.toMessage(message));
        } catch (Exception e) {
            log.error("error : order-local record : {}", record);
            log.error(e.getMessage(), e);

            kafkaProducerService.sendToDlq(message, e);

            throw e;
        }
    }
}
