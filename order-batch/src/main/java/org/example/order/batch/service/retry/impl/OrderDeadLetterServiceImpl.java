package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.event.DlqMessage;
import org.example.order.common.exception.code.CommonExceptionCode;
import org.example.order.common.code.type.DlqType;
import org.example.order.common.utils.jackson.ObjectMapperUtils;
import org.example.order.core.application.order.event.message.OrderApiEvent;
import org.example.order.core.application.order.event.message.OrderCrudEvent;
import org.example.order.core.application.order.event.message.OrderLocalEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaConsumerProperties.class})
public class OrderDeadLetterServiceImpl implements OrderDeadLetterService {
    private final KafkaProducerService kafkaProducerService;
    private final KafkaConsumerProperties kafkaConsumerProperties;

    @Override
    public void retry(Object message) {
        DlqType type = ObjectMapperUtils.getFieldValueFromString(message.toString(), "type", DlqType.class);
        log.info("dlq type : {}", type);

        switch (type) {
            case ORDER_LOCAL -> OrderLocal(message);
            case ORDER_API -> OrderApi(message);
            case ORDER_CRUD -> OrderCrud(message);
        }
    }

    private void OrderLocal(Object message) {
        List<Integer> discardCode = Arrays.asList(CommonExceptionCode.INVALID_REQUEST.getCode(), BatchExceptionCode.EMPTY_MESSAGE.getCode());
        OrderLocalEvent orderLocalEvent = convertMessage(message, OrderLocalEvent.class);
        orderLocalEvent.increaseFailedCount();

        if (invalid(orderLocalEvent) || discardCode.contains(orderLocalEvent.getError().getCode())) {
            kafkaProducerService.sendToDiscard(orderLocalEvent);
        } else {
            kafkaProducerService.sendToLocal(orderLocalEvent);
        }
    }

    private void OrderApi(Object message) {
        OrderApiEvent orderApiEvent = convertMessage(message, OrderApiEvent.class);
        orderApiEvent.increaseFailedCount();

        if (invalid(orderApiEvent)) {
            kafkaProducerService.sendToDiscard(orderApiEvent);
        } else {
            kafkaProducerService.sendToOrderApi(orderApiEvent);
        }
    }

    private void OrderCrud(Object message) {
        OrderCrudEvent orderCrudEvent = convertMessage(message, OrderCrudEvent.class);
        orderCrudEvent.increaseFailedCount();

        if (invalid(orderCrudEvent)) {
            kafkaProducerService.sendToDiscard(orderCrudEvent);
        } else {
            kafkaProducerService.sendToOrderCrud(orderCrudEvent);
        }
    }

    private <T> T convertMessage(Object message, Class<T> clz) {
        return ObjectMapperUtils.valueToObject(message, clz);
    }

    private <T extends DlqMessage> boolean invalid(T message) {
        return message.discard(kafkaConsumerProperties.getOption().getMaxFailCount());
    }
}
