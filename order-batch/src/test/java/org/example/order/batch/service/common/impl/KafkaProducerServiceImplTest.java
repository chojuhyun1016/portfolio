/*
package org.example.order.batch.service.common.impl;

import static org.mockito.Mockito.*;

import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.common.core.monitoring.message.MonitoringMessage;
import org.example.order.core.infra.messaging.order.code.MessageCategory;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

*/
/**
 * KafkaProducerServiceImpl 라우팅 테스트
 * - 카테고리별 토픽 선택 및 전송 위임
 *//*

class KafkaProducerServiceImplTest {

    @Test
    @DisplayName("sendToLocal → ORDER_LOCAL로 전송")
    void sendToLocal_should_route_ORDER_LOCAL() {
        KafkaProducerCluster cluster = mock(KafkaProducerCluster.class);
        KafkaTopicProperties topics = mock(KafkaTopicProperties.class);
        when(topics.getName(MessageCategory.ORDER_LOCAL)).thenReturn("ORDER_LOCAL");

        KafkaProducerServiceImpl svc = new KafkaProducerServiceImpl(cluster, topics);
        svc.sendToLocal(new OrderLocalMessage());

        verify(cluster, times(1)).sendMessage(any(OrderLocalMessage.class), eq("ORDER_LOCAL"));
    }

    @Test
    @DisplayName("sendToOrderApi → ORDER_API로 전송")
    void sendToOrderApi_should_route_ORDER_API() {
        KafkaProducerCluster cluster = mock(KafkaProducerCluster.class);
        KafkaTopicProperties topics = mock(KafkaTopicProperties.class);
        when(topics.getName(MessageCategory.ORDER_API)).thenReturn("ORDER_API");

        KafkaProducerServiceImpl svc = new KafkaProducerServiceImpl(cluster, topics);
        svc.sendToOrderApi(new OrderApiMessage());

        verify(cluster, times(1)).sendMessage(any(OrderApiMessage.class), eq("ORDER_API"));
    }

    @Test
    @DisplayName("sendToOrderCrud → ORDER_CRUD로 전송")
    void sendToOrderCrud_should_route_ORDER_CRUD() {
        KafkaProducerCluster cluster = mock(KafkaProducerCluster.class);
        KafkaTopicProperties topics = mock(KafkaTopicProperties.class);
        when(topics.getName(MessageCategory.ORDER_CRUD)).thenReturn("ORDER_CRUD");

        KafkaProducerServiceImpl svc = new KafkaProducerServiceImpl(cluster, topics);
        svc.sendToOrderCrud(new OrderCrudMessage());

        verify(cluster, times(1)).sendMessage(any(OrderCrudMessage.class), eq("ORDER_CRUD"));
    }

    @Test
    @DisplayName("sendToDiscard → ORDER_ALARM로 전송")
    void sendToDiscard_should_route_ORDER_ALARM() {
        KafkaProducerCluster cluster = mock(KafkaProducerCluster.class);
        KafkaTopicProperties topics = mock(KafkaTopicProperties.class);
        when(topics.getName(MessageCategory.ORDER_ALARM)).thenReturn("ORDER_ALARM");

        KafkaProducerServiceImpl svc = new KafkaProducerServiceImpl(cluster, topics);
        DlqMessage dummy = mock(DlqMessage.class);
        svc.sendToDiscard(dummy);

        verify(cluster, times(1)).sendMessage(any(MonitoringMessage.class), eq("ORDER_ALARM"));
    }

    @Test
    @DisplayName("sendToDlq(단건) → ORDER_DLQ로 전송")
    void sendToDlq_single_should_route_ORDER_DLQ() {
        KafkaProducerCluster cluster = mock(KafkaProducerCluster.class);
        KafkaTopicProperties topics = mock(KafkaTopicProperties.class);
        when(topics.getName(MessageCategory.ORDER_DLQ)).thenReturn("ORDER_DLQ");

        KafkaProducerServiceImpl svc = new KafkaProducerServiceImpl(cluster, topics);
        DlqMessage dummy = mock(DlqMessage.class);
        svc.sendToDlq(dummy, new RuntimeException("x"));

        verify(cluster, times(1)).sendMessage(eq(dummy), eq("ORDER_DLQ"));
    }
}
*/
