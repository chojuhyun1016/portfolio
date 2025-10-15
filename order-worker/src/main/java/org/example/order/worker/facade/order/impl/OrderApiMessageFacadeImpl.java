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
 * - API 호출로 데이터를 조회하고 CRUD 메시지를 발행
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

            if (dto == null) {
                throw new IllegalArgumentException("OrderApiConsumerDto is null");
            }

            dto.validate();

            OrderSyncDto orderDto = webClientService.findOrderListByOrderId(dto.getId());

            if (orderDto == null) {
                throw new IllegalStateException("Order API returned empty order for id=" + dto.getId());
            }

            OrderPayload payload = toPayload(orderDto);

            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.of(dto.getOperation(), payload));

        } catch (Exception e) {
            log.error("error : order api dto : {}", dto, e);

            throw e;
        }
    }

    private OrderPayload toPayload(OrderSyncDto o) {
        return new OrderPayload(
                o.getOrderId(),
                o.getOrderNumber(),
                o.getUserId(),
                o.getUserNumber(),
                o.getOrderPrice()
        );
    }
}
