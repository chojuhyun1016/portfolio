// src/main/java/org/example/order/worker/service/common/impl/KafkaProducerServiceImpl.java
package org.example.order.worker.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.core.exception.message.CustomErrorMessage;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.infra.messaging.order.code.MessageCategory;
import org.example.order.core.infra.messaging.order.message.*;
import org.example.order.worker.service.common.KafkaProducerService;
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
    public void sendToOrderRemote(OrderCloseMessage message) {
        send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_REMOTE));
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
    public <T extends DlqMessage> void sendToDlq(T message, Exception currentException) {
        if (ObjectUtils.isEmpty(message)) {
            return;
        }

        try {
            if (currentException instanceof CommonException ce) {
                message.fail(CustomErrorMessage.toMessage(ce.getCode(), ce));
            } else {
                message.fail(CustomErrorMessage.toMessage(currentException));
            }

            String dlqTopic = kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ);

            if (ObjectUtils.isEmpty(dlqTopic)) {
                log.error("DLQ topic name is empty. message={}", message);

                return;
            }

            log.info("Sending message to DLQ: {}", dlqTopic);

            send(message, dlqTopic);
        } catch (Exception e) {
            log.error("error : sending message to DLQ failed. message={}", message, e);
        }
    }

    private void send(Object message, String topic) {
        if (ObjectUtils.isEmpty(topic)) {
            log.error("Kafka topic is empty. skip sending. message={}", message);

            return;
        }

        cluster.sendMessage(message, topic);
    }
}
