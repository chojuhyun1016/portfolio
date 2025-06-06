package org.example.order.worker.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.messaging.order.code.MessageCategory;
import org.example.order.core.messaging.order.message.OrderApiMessage;
import org.example.order.core.messaging.order.message.OrderCrudMessage;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.example.order.core.messaging.order.message.OrderCloseMessage;
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
            message.fail(currentException);
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
