//package org.example.order.worker.facade.order.impl;
//
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.example.order.common.core.messaging.message.DlqMessage;
//import org.example.order.worker.exception.DatabaseExecuteException;
//import org.example.order.worker.exception.WorkerExceptionCode;
//import org.example.order.worker.service.common.KafkaProducerService;
//import org.example.order.worker.service.order.OrderService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * Kafka JsonDeserializer 실환경과 유사하게 value=Map 으로 시뮬레이션:
// * - 성공 경로: DB 반영 + 원격 발행
// * - 실패 경로: 서비스 예외 → 그룹(List) DLQ 전송
// */
//class OrderCrudMessageFacadeImplTest {
//
//    private Map<String, Object> buildCrudMessageMap(MessageMethodType methodType, long orderId, boolean ignoredFailureFlag) {
//        Map<String, Object> order = new LinkedHashMap<>();
//        order.put("orderId", orderId);
//        order.put("failure", ignoredFailureFlag); // @JsonIgnore 가정: 실제로는 무시됨
//
//        Map<String, Object> dto = new LinkedHashMap<>();
//        dto.put("order", order);
//
//        Map<String, Object> root = new LinkedHashMap<>();
//        root.put("methodType", methodType.name());
//        root.put("dto", dto);
//
//        return root;
//    }
//
//    @Test
//    @DisplayName("성공: 그룹핑 후 DB 반영 + remote 발행(2건)")
//    void execute_success() {
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        OrderService orderService = mock(OrderService.class);
//        OrderCrudMessageFacadeImpl facade = new OrderCrudMessageFacadeImpl(producer, orderService);
//
//        Map<String, Object> m1 = buildCrudMessageMap(MessageMethodType.POST, 1L, false);
//        Map<String, Object> m2 = buildCrudMessageMap(MessageMethodType.POST, 2L, false);
//
//        ConsumerRecord<String, Object> r1 = new ConsumerRecord<>("t", 0, 0, "k1", m1);
//        ConsumerRecord<String, Object> r2 = new ConsumerRecord<>("t", 0, 1, "k2", m2);
//
//        facade.executeOrderCrud(List.of(r1, r2));
//
//        verify(orderService, times(1)).execute(eq(MessageMethodType.POST), anyList());
//        verify(producer, times(2)).sendToOrderRemote(any());
//        verify(producer, never()).sendToDlq(anyList(), any());
//        verify(producer, never()).sendToDlq(any(DlqMessage.class), any());
//    }
//
//    @Test
//    @DisplayName("실패: OrderService.execute 예외 → 그룹(List) DLQ 호출")
//    void execute_group_failure_to_dlq() {
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        OrderService orderService = mock(OrderService.class);
//        OrderCrudMessageFacadeImpl facade = new OrderCrudMessageFacadeImpl(producer, orderService);
//
//        Map<String, Object> ok = buildCrudMessageMap(MessageMethodType.PUT, 1L, false);
//        Map<String, Object> ng = buildCrudMessageMap(MessageMethodType.PUT, 2L, true);
//
//        ConsumerRecord<String, Object> r1 = new ConsumerRecord<>("t", 0, 0, "k1", ok);
//        ConsumerRecord<String, Object> r2 = new ConsumerRecord<>("t", 0, 1, "k2", ng);
//
//        doThrow(new DatabaseExecuteException(WorkerExceptionCode.MESSAGE_UPDATE_FAILED))
//                .when(orderService).execute(eq(MessageMethodType.PUT), anyList());
//
//        try {
//            facade.executeOrderCrud(List.of(r1, r2));
//        } catch (DatabaseExecuteException ignore) {
//        }
//
//        @SuppressWarnings("unchecked")
//        ArgumentCaptor<List<? extends DlqMessage>> listCaptor = ArgumentCaptor.forClass(List.class);
//        verify(producer, times(1)).sendToDlq(listCaptor.capture(), any(DatabaseExecuteException.class));
//        assertThat(listCaptor.getValue()).hasSize(2);
//
//        verify(producer, never()).sendToOrderRemote(any());
//        verify(producer, never()).sendToDlq(any(DlqMessage.class), any());
//    }
//}
