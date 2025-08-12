package org.example.order.api.master.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.core.messaging.order.code.MessageCategory;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaProducerCluster cluster;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void sendToOrder(OrderLocalMessage message) {
        send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL));
    }

    private void send(Object message, String topic) {
        cluster.sendMessage(message, topic);
    }
}
