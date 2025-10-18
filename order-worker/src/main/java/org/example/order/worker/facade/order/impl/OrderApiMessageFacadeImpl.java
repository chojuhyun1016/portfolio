package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.worker.dto.consumer.OrderApiConsumerDto;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.common.WebClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderApiMessageFacadeImpl
 * - API 호출 후 CRUD 메시지 발행
 * - 예외 발생 시 Envelope의 원본 헤더를 사용해 DLQ 전송
 * - OrderPayload는 생성/수정 메타를 포함해 구성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageFacadeImpl implements OrderApiMessageFacade {

    private final KafkaProducerService kafkaProducerService;
    private final WebClientService webClientService;

    @Transactional
    @Override
    public void requestApi(ConsumerEnvelope<OrderApiConsumerDto> envelope) {
        OrderApiConsumerDto dto = envelope.getPayload();

        try {
            if (dto == null) {
                throw new IllegalArgumentException("OrderApiConsumerDto is null");
            }

            dto.validate();

            log.info("[API->CRUD] requestApi id={}", dto.getId());

            // 1) API 조회
            LocalOrderSync orderDto = webClientService.findOrderListByOrderId(dto.getId());
            if (orderDto == null) {
                throw new IllegalStateException("Order API returned empty order for id=" + dto.getId());
            }

            // 2) 내부 DTO -> 계약 Payload (메타 포함)
            OrderPayload payload = toPayload(orderDto);

            // 3) CRUD 메시지 발행
            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.of(dto.getOperation(), payload));

        } catch (Exception e) {
            log.error("order-api failed. id={} cause={}", dto == null ? null : dto.getId(), e.toString());

            kafkaProducerService.sendToDlq(dto, envelope.getHeaders(), e);
        }
    }

    private OrderPayload toPayload(LocalOrderSync o) {
        return new OrderPayload(
                o.getId(),
                o.getOrderId(),
                o.getOrderNumber(),
                o.getUserId(),
                o.getUserNumber(),
                o.getOrderPrice(),
                o.getDeleteYn(),
                o.getVersion(),
                o.getCreatedUserId(),
                o.getCreatedUserType(),
                o.getCreatedDatetime(),
                o.getModifiedUserId(),
                o.getModifiedUserType(),
                o.getModifiedDatetime(),
                o.getPublishedTimestamp()
        );
    }
}
