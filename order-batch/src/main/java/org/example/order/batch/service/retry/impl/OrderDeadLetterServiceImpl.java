package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.property.KafkaConsumerProperties;
import org.example.order.common.application.message.DlqMessage;
import org.example.order.common.code.CommonExceptionCode;
import org.example.order.common.code.DlqType;
import org.example.order.common.utils.jackson.ObjectMapperUtils;
import org.example.order.core.application.message.OrderApiMessage;
import org.example.order.core.application.message.OrderCrudMessage;
import org.example.order.core.application.message.OrderLocalMessage;
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
        OrderLocalMessage orderLocalMessage = convertMessage(message, OrderLocalMessage.class);
        orderLocalMessage.increaseFailedCount();

        if (invalid(orderLocalMessage) || discardCode.contains(orderLocalMessage.getError().getCode())) {
            kafkaProducerService.sendToDiscard(orderLocalMessage);
        } else {
            kafkaProducerService.sendToLocal(orderLocalMessage);
        }
    }

    private void OrderApi(Object message) {
        OrderApiMessage orderApiMessage = convertMessage(message, OrderApiMessage.class);
        orderApiMessage.increaseFailedCount();

        if (invalid(orderApiMessage)) {
            kafkaProducerService.sendToDiscard(orderApiMessage);
        } else {
            kafkaProducerService.sendToOrderApi(orderApiMessage);
        }
    }

    private void OrderCrud(Object message) {
        OrderCrudMessage orderCrudMessage = convertMessage(message, OrderCrudMessage.class);
        orderCrudMessage.increaseFailedCount();

        if (invalid(orderCrudMessage)) {
            kafkaProducerService.sendToDiscard(orderCrudMessage);
        } else {
            kafkaProducerService.sendToOrderCrud(orderCrudMessage);
        }
    }

    private <T> T convertMessage(Object message, Class<T> clz) {
        return ObjectMapperUtils.valueToObject(message, clz);
    }

    private <T extends DlqMessage> boolean invalid(T message) {
        return message.discard(kafkaConsumerProperties.getOption().getMaxFailCount());
    }
}
