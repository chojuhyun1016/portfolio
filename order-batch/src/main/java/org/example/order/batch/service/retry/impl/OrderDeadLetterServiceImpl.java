package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.core.messaging.code.DlqType;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.messaging.order.message.OrderApiMessage;
import org.example.order.core.messaging.order.message.OrderCrudMessage;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static org.example.order.core.messaging.order.code.DlqOrderType.*;

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
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }

    private void OrderLocal(Object message) {
        OrderLocalMessage orderLocalMessage = convertMessage(message, OrderLocalMessage.class);
        orderLocalMessage.increaseFailedCount();

        if (invalid(orderLocalMessage)) {
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
