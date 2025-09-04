package org.example.order.api.master.service.common.impl;

import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.core.infra.messaging.order.code.MessageCategory;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class KafkaProducerServiceImplTest {

    @Test
    @DisplayName("KafkaProducerService: ORDER_LOCAL 토픽으로 라우팅")
    void sendToOrder_should_route_to_topic() {
        KafkaProducerCluster cluster = mock(KafkaProducerCluster.class);
        KafkaTopicProperties topics = mock(KafkaTopicProperties.class);
        when(topics.getName(MessageCategory.ORDER_LOCAL)).thenReturn("ORDER_LOCAL");

        KafkaProducerServiceImpl svc = new KafkaProducerServiceImpl(cluster, topics);

        OrderLocalMessage message = mock(OrderLocalMessage.class);
        svc.sendToOrder(message);

        verify(topics, times(1)).getName(MessageCategory.ORDER_LOCAL);
        verify(cluster, times(1)).sendMessage(message, "ORDER_LOCAL");
        verifyNoMoreInteractions(cluster, topics);
    }
}
