package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.utils.jackson.ObjectMapperUtils;
import org.example.order.core.application.event.OrderApiEvent;
import org.example.order.core.application.event.OrderLocalEvent;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageFacadeImpl implements OrderLocalMessageFacade {
    private final KafkaProducerService kafkaProducerService;

    @Override
    public void sendOrderApiTopic(ConsumerRecord<String, Object> record) {
        OrderLocalEvent message = null;

        try {
            message = ObjectMapperUtils.valueToObject(record.value(), OrderLocalEvent.class);

            log.debug("order-local record : {}", message);

            message.validation();

            kafkaProducerService.sendToOrderApi(OrderApiEvent.toMessage(message));
        } catch (Exception e) {
            // 비정상 메시지 DLQ 처리
            log.error("error : order-local record : {}", record);
            log.error(e.getMessage(), e);
            kafkaProducerService.sendToDlq(message, e);

            throw e;
        }
    }
}
