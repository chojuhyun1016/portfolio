package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.common.WebClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderApiMessageFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - API 호출 → DTO 조합 → CRUD 발행.
 * MDC 전략
 * - 리스너에서 @Correlate 로 traceId/orderId가 이미 세팅됨(동기 호출 경로).
 * <p>
 * [변경 사항]
 * - ObjectMapperUtils 변환 제거, POJO 직접 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageFacadeImpl implements OrderApiMessageFacade {

    private final KafkaProducerService kafkaProducerService;
    private final WebClientService webClientService;

    @Transactional
    @Override
    public void requestApi(OrderApiMessage message) {

        try {
            log.info("requestApi : message : {}", message);

            if (message == null) {
                throw new IllegalArgumentException("OrderApiMessage is null");
            }

            OrderDto dto = webClientService.findOrderListByOrderId(message.getId());
//            dto.updatePublishedTimestamp(message.getPublishedTimestamp());

//            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.toMessage(message, dto));
        } catch (Exception e) {
            log.error("error : order api message : {}", message);
            log.error(e.getMessage(), e);

            kafkaProducerService.sendToDlq(message, e);

            throw e;
        }
    }
}
