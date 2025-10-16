package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.core.application.order.dto.internal.OrderSyncDto;
import org.example.order.worker.dto.consumer.OrderApiConsumerDto;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.common.WebClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderApiMessageFacadeImpl
 * - API 호출 후 CRUD 메시지 발행
 * - OrderPayload를 생성/수정 메타까지 채워서 보낸다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageFacadeImpl implements OrderApiMessageFacade {

    private final KafkaProducerService kafkaProducerService;
    private final WebClientService webClientService;

    @Transactional
    @Override
    public void requestApi(OrderApiConsumerDto dto) {
        try {
            log.info("requestApi : dto : {}", dto);
            if (dto == null) throw new IllegalArgumentException("OrderApiConsumerDto is null");

            dto.validate();

            // 1) API 조회
            OrderSyncDto orderDto = webClientService.findOrderListByOrderId(dto.getId());
            if (orderDto == null) {
                throw new IllegalStateException("Order API returned empty order for id=" + dto.getId());
            }

            // 2) 내부 DTO -> 계약 Payload (메타 포함)
            OrderPayload payload = toPayload(orderDto);

            // 3) CRUD 메시지 발행
            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.of(dto.getOperation(), payload));
        } catch (Exception e) {
            log.error("error : order api dto : {}", dto, e);
            throw e;
        }
    }

    private OrderPayload toPayload(OrderSyncDto o) {
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
