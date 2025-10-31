package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * DLQ 메시지 유형별 재처리 (기존 방식 유지)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaConsumerProperties.class})
public class OrderDeadLetterServiceImpl implements OrderDeadLetterService {

    private final KafkaProducerService kafkaProducerService;
    private final KafkaConsumerProperties kafkaConsumerProperties;

    @Override
    public void retry(Object message) {
        MessageOrderType type;

        try {
            type = ObjectMapperUtils.getFieldValueFromString(String.valueOf(message), "type", MessageOrderType.class);
        } catch (Exception e) {
            log.error("DLQ retry: failed to resolve type from payload: {}", message);

            throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }

        log.info("DLQ 처리 시작 - Type: {}", type);

        switch (type) {
            case ORDER_LOCAL -> retryMessage(message, OrderLocalMessage.class);
            case ORDER_API -> retryMessage(message, OrderApiMessage.class);
            case ORDER_CRUD -> retryMessage(message, OrderCrudMessage.class);
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }

    private <T> void retryMessage(Object rawMessage, Class<T> clazz) {
        DeadLetter<T> dlqMessage = ObjectMapperUtils.valueToObject(rawMessage,
                ObjectMapperUtils.constructParametricType(DeadLetter.class, clazz));

        if (dlqMessage == null || dlqMessage.payload() == null) {
            log.warn("skip: empty DLQ payload (class={})", clazz.getSimpleName());

            return;
        }

        if (shouldDiscard(dlqMessage)) {
            kafkaProducerService.sendToDiscard(dlqMessage);
        } else {
            resend(dlqMessage.payload());
        }
    }

    private boolean shouldDiscard(DeadLetter<?> message) {
        try {
            // 실패 횟수 초과 여부 판단(옵션 기반, 필요 시 확장)
            return message.error() != null
                    && kafkaConsumerProperties != null
                    && kafkaConsumerProperties.getOption() != null
                    && kafkaConsumerProperties.getOption().getMaxFailCount() != null
                    && Boolean.TRUE.equals(message.error().isExceeded(kafkaConsumerProperties.getOption().getMaxFailCount()));
        } catch (Throwable ignore) {
            return false;
        }
    }

    private void resend(Object payload) {
        if (payload instanceof OrderLocalMessage m) {
            kafkaProducerService.sendToLocal(m);
        } else if (payload instanceof OrderApiMessage m) {
            kafkaProducerService.sendToOrderApi(m);
        } else if (payload instanceof OrderCrudMessage m) {
            kafkaProducerService.sendToOrderCrud(m);
        } else {
            log.warn("Unsupported payload class for resend: {}", (payload == null ? null : payload.getClass()));
            throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }
}
