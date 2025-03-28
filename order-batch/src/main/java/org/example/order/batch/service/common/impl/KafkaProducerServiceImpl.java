package org.example.order.batch.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.client.kafka.config.property.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.application.message.CustomErrorMessage;
import org.example.order.common.application.message.DlqMessage;
import org.example.order.common.application.message.MonitoringMessage;
import org.example.order.common.code.MessageCategory;
import org.example.order.common.code.MonitoringType;
import org.example.order.common.exception.CommonException;
import org.example.order.core.application.message.OrderApiMessage;
import org.example.order.core.application.message.OrderCrudMessage;
import org.example.order.core.application.message.OrderLocalMessage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaProducerCluster cluster;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void sendToLocal(OrderLocalMessage message) {
        send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL));
    }

    @Override
    public void sendToOrderApi(OrderApiMessage message) {
        send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_API));
    }

    @Override
    public void sendToOrderCrud(OrderCrudMessage message) {
        send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_CRUD));
    }

    @Override
    public <T extends DlqMessage> void sendToDlq(List<T> messages, Exception currentException) {
        if (ObjectUtils.isEmpty(messages)) {
            return;
        }

        for (T message : messages) {
            sendToDlq(message, currentException);
        }
    }

    @Override
    public <T extends DlqMessage> void sendToDiscard(T message) {
        log.info("Sending message to discard topic");
        send(MonitoringMessage.toMessage(MonitoringType.ERROR, message), kafkaTopicProperties.getName(MessageCategory.ORDER_ALARM));
    }

    @Override
    public <T extends DlqMessage> void sendToDlq(T message, Exception currentException) {
        if (ObjectUtils.isEmpty(message)) {
            return;
        }

        try {
            if (currentException instanceof CommonException commonException) {
                message.fail(CustomErrorMessage.toMessage(commonException.getCode(), commonException));
            } else {
                message.fail(CustomErrorMessage.toMessage(currentException));
            }
            log.info("Sending message to dead letter topic");

            send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ));
        } catch (Exception e) {
            log.error("error : send message to order-dead-letter failed : {}", message);
            log.error(e.getMessage(), e);
        }
    }

    private void send(Object message, String topic) {
        cluster.sendMessage(message, topic);
    }
}
