package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.worker.dto.consumer.OrderLocalConsumerDto;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.springframework.stereotype.Component;

/**
 * OrderLocalMessageFacadeImpl
 * - Local 컨슈머 DTO를 계약 메시지로 변환해 API 토픽에 전송
 * - 정상 전송 시 헤더를 수정하지 않음
 * - 실패 시 Envelope의 원본 헤더를 DLQ 전송에 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageFacadeImpl implements OrderLocalMessageFacade {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public void sendOrderApiTopic(ConsumerEnvelope<OrderLocalConsumerDto> envelope) {
        OrderLocalConsumerDto dto = envelope.getPayload();

        try {
            if (dto == null) {
                throw new IllegalArgumentException("OrderLocalConsumerDto is null");
            }

            dto.validate();

            log.info("[LOCAL->API] orderId={}", dto.getId());

            OrderApiMessage msg = new OrderApiMessage(
                    dto.getOperation(),
                    MessageOrderType.ORDER_API,
                    dto.getId(),
                    dto.getPublishedTimestamp()
            );

            kafkaProducerService.sendToOrderApi(msg);

        } catch (Exception e) {
            log.error("order-local failed. orderId={} cause={}",
                    dto == null ? null : dto.getId(), e.toString());

            kafkaProducerService.sendToDlq(dto, envelope.getHeaders(), e);
        }
    }
}
