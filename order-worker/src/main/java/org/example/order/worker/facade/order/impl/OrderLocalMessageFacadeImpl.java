package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.springframework.stereotype.Component;

/**
 * OrderLocalMessageFacadeImpl
 * ------------------------------------------------------------------------
 * 목적
 * - 로컬 메시지 → API 메시지 변환/발행.
 * MDC 전략
 * - 리스너에서 @Correlate 로 traceId/orderId가 이미 세팅됨(동기 호출 경로).
 * <p>
 * [변경 사항]
 * - ObjectMapper 변환 제거, POJO 직접 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageFacadeImpl implements OrderLocalMessageFacade {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public void sendOrderApiTopic(OrderLocalMessage message) {

        try {
            log.info("sendOrderApiTopic : message : {}", message);

            if (message == null) {
                throw new IllegalArgumentException("OrderLocalMessage is null");
            }

            message.validation();

            kafkaProducerService.sendToOrderApi(OrderApiMessage.toMessage(message));
        } catch (Exception e) {
            log.error("error : order-local message : {}", message);
            log.error(e.getMessage(), e);

            kafkaProducerService.sendToDlq(message, e);

            throw e;
        }
    }
}
