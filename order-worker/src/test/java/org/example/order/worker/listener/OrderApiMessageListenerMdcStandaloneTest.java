//package org.example.order.worker.listener;
//
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.example.order.common.support.logging.CorrelationAspect;
//import org.example.order.worker.facade.order.OrderApiMessageFacade;
//import org.example.order.worker.listener.order.OrderApiMessageListener;
//import org.example.order.worker.listener.order.impl.OrderApiMessageListenerImpl;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.slf4j.MDC;
//import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
//import org.springframework.kafka.support.Acknowledgment;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Standalone 단위 테스트:
// * - 스프링 컨텍스트 없이 @Correlate 적용 확인 (AspectJ 프록시)
// * - Kafka 헤더 traceId 유무와 상관없이, @Correlate가 orderId로 traceId 덮어쓰는지 검증
// * - 프록시는 JDK 동적 프록시가 생성되므로 '인터페이스' 타입으로 캐스팅한다.
// */
//class OrderApiMessageListenerMdcStandaloneTest {
//
//    @Test
//    @DisplayName("@Correlate: record.value().id → traceId/orderId 주입")
//    void correlate_should_set_traceId_to_message_id() {
//        // given
//        OrderApiMessageFacade facade = Mockito.mock(OrderApiMessageFacade.class);
//        OrderApiMessageListenerImpl target = new OrderApiMessageListenerImpl(facade);
//
//        // Aspect 적용
//        AspectJProxyFactory factory = new AspectJProxyFactory(target);
//        factory.addAspect(new CorrelationAspect());
//
//        // JDK 동적 프록시 → getProxy()로 받고 '인터페이스'로 캐스팅
//        OrderApiMessageListener proxied = (OrderApiMessageListener) factory.getProxy();
//
//        // Kafka 레코드(실환경 JsonDeserializer 유사: value=Map)
//        Map<String, Object> payload = new LinkedHashMap<>();
//        payload.put("id", 777L);
//        payload.put("publishedTimestamp", System.currentTimeMillis());
//
//        // 간결한 생성자 사용(topic, partition, offset, key, value)
//        ConsumerRecord<String, Object> record =
//                new ConsumerRecord<>("ORDER_API", 0, 0L, "k", payload);
//
//        Acknowledgment ack = Mockito.mock(Acknowledgment.class);
//
//        // facade 호출 직전 MDC 단언
//        Mockito.doAnswer(inv -> {
//            assertThat(MDC.get("traceId")).isEqualTo("777");
//            assertThat(MDC.get("orderId")).isEqualTo("777");
//            return null;
//        }).when(facade).requestApi(Mockito.any());
//
//        // when
////        proxied.orderApi(record.value(), ack);
//
//        // then
//        Mockito.verify(ack, Mockito.times(1)).acknowledge();
//    }
//}
