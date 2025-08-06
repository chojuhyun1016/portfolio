package org.example.order.batch.service.retry.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.service.retry.exception.BatchExceptionCode;
import org.example.order.batch.service.retry.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.messaging.order.code.DlqOrderType;
import org.example.order.core.messaging.order.message.OrderApiMessage;
import org.example.order.core.messaging.order.message.OrderCrudMessage;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaConsumerProperties.class})
public class OrderDeadLetterServiceImpl implements OrderDeadLetterService {

    private final KafkaProducerService kafkaProducerService;
    private final KafkaConsumerProperties kafkaConsumerProperties;

    @Override
    public void retry(Object message) {
        DlqOrderType type = ObjectMapperUtils.getFieldValueFromString(message.toString(), "type", DlqOrderType.class);
        log.info("DLQ 처리 시작 - Type: {}", type);

        switch (type) {
            case ORDER_LOCAL -> retryMessage(message, OrderLocalMessage.class, kafkaProducerService::sendToLocal);
            case ORDER_API   -> retryMessage(message, OrderApiMessage.class, kafkaProducerService::sendToOrderApi);
            case ORDER_CRUD  -> retryMessage(message, OrderCrudMessage.class, kafkaProducerService::sendToOrderCrud);
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }

    private <T extends DlqMessage> void retryMessage(Object rawMessage, Class<T> clazz, java.util.function.Consumer<T> retrySender) {
        T dlqMessage = ObjectMapperUtils.valueToObject(rawMessage, clazz);
        dlqMessage.increaseFailedCount();

        if (shouldDiscard(dlqMessage)) {
            kafkaProducerService.sendToDiscard(dlqMessage);
        } else {
            retrySender.accept(dlqMessage);
        }
    }

    private <T extends DlqMessage> boolean shouldDiscard(T message) {
        return message.discard(kafkaConsumerProperties.getOption().getMaxFailCount());
    }
}
