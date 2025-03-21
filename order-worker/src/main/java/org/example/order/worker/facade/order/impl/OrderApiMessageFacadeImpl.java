package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.ObjectMapperUtils;
import org.example.order.core.application.dto.order.OrderDto;
import org.example.order.core.application.message.order.OrderApiMessage;
import org.example.order.core.application.message.order.OrderCrudMessage;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.common.OrderWebClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageFacadeImpl implements OrderApiMessageFacade {
    private final KafkaProducerService kafkaProducerService;
    private final OrderWebClientService webClientService;

    @Transactional
    @Override
    public void requestApi(Object record) {
        OrderApiMessage message = null;

        try {
            message = ObjectMapperUtils.valueToObject(record, OrderApiMessage.class);

            // api 호출
            OrderDto dto = webClientService.findOrderListByOrderId(message.getId());
            dto.postUpdate(message.getPublishedTimestamp());

            // 메세지 발행
            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.toMessage(message, dto));
        } catch (Exception e) {
            log.error("error : order api record : {}", record);
            log.error(e.getMessage(), e);
            kafkaProducerService.sendToDlq(message, e);

            throw e;
        }
    }
}
