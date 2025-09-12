package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.common.OrderWebClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.example.order.common.support.logging.Correlate;

/**
 * OrderApiMessageFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - API 호출 → DTO 조합 → CRUD 발행.
 * MDC 전략
 * - 리스너에서 이미 @Correlate 적용되지만, 파사드에서도 도메인 키 기반 추적을 한 번 더 보강(방어적).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageFacadeImpl implements OrderApiMessageFacade {

    private final KafkaProducerService kafkaProducerService;
    private final OrderWebClientService webClientService;

    @Transactional
    @Override
    @Correlate(
            key = "T(org.example.order.common.support.json.ObjectMapperUtils)" +
                    ".valueToObject(#record, T(org.example.order.core.infra.messaging.order.message.OrderApiMessage)).id",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void requestApi(Object record) {

        OrderApiMessage message = null;

        try {
            message = ObjectMapperUtils.valueToObject(record, OrderApiMessage.class);

            OrderDto dto = webClientService.findOrderListByOrderId(message.getId());
            dto.updatePublishedTimestamp(message.getPublishedTimestamp());

            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.toMessage(message, dto));
        } catch (Exception e) {
            log.error("error : order api record : {}", record);
            log.error(e.getMessage(), e);

            kafkaProducerService.sendToDlq(message, e);

            throw e;
        }
    }
}
