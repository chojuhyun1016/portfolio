//package org.example.order.worker.listener.order.impl;
//
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
//import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
//import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
//import org.example.order.worker.facade.order.OrderApiMessageFacade;
//import org.example.order.worker.facade.order.OrderCrudMessageFacade;
//import org.example.order.worker.facade.order.OrderLocalMessageFacade;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.kafka.support.Acknowledgment;
//
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//
///**
// * 리스너 3종: 파사드 위임 + ack 호출 보장
// * Kafka, 컨테이너 미사용
// */
//class OrderListenersUnitTest {
//
//    @Test
//    @DisplayName("Local Listener: facade 호출 + ack")
//    void orderLocal_calls_facade_and_acks() {
//        OrderLocalMessageFacade facade = mock(OrderLocalMessageFacade.class);
//        var listener = new OrderLocalMessageListenerImpl(facade);
//
//        OrderLocalMessage payload = new OrderLocalMessage();
//        var record = new ConsumerRecord<>("t", 0, 0, "k", payload);
//        Acknowledgment ack = mock(Acknowledgment.class);
//
//        listener.orderLocal(record, ack);
//
//        verify(facade, times(1)).sendOrderApiTopic(payload);
//        verify(ack, times(1)).acknowledge();
//    }
//
//    @Test
//    @DisplayName("API Listener: facade 호출 + ack")
//    void orderApi_calls_facade_and_acks() {
//        OrderApiMessageFacade facade = mock(OrderApiMessageFacade.class);
//        var listener = new OrderApiMessageListenerImpl(facade);
//
//        OrderApiMessage payload = new OrderApiMessage();
//        var record = new ConsumerRecord<>("t", 0, 0, "k", payload);
//        Acknowledgment ack = mock(Acknowledgment.class);
//
//        listener.orderApi(record, ack);
//
//        verify(facade, times(1)).requestApi(payload);
//        verify(ack, times(1)).acknowledge();
//    }
//
//    @Test
//    @DisplayName("CRUD Listener: facade 호출 + ack")
//    void orderCrud_calls_facade_and_acks() {
//        OrderCrudMessageFacade facade = mock(OrderCrudMessageFacade.class);
//        var listener = new OrderCrudMessageListenerImpl(facade);
//
//        OrderCrudMessage m1 = new OrderCrudMessage();
//        OrderCrudMessage m2 = new OrderCrudMessage();
//
//        var r1 = new ConsumerRecord<>("t", 0, 0, "k1", m1);
//        var r2 = new ConsumerRecord<>("t", 0, 1, "k2", m2);
//
//        Acknowledgment ack = mock(Acknowledgment.class);
//
//        listener.executeOrderCrud(List.of(r1, r2), ack);
//
//        verify(facade, times(1)).executeOrderCrud(anyList());
//        verify(ack, times(1)).acknowledge();
//    }
//}
