//package org.example.order.worker.facade.order.impl;
//
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.example.order.common.core.messaging.code.Operation;
//import org.example.order.common.core.messaging.message.DlqMessage;
//import org.example.order.core.application.order.dto.internal.OrderSyncDto;
//import org.example.order.core.application.order.dto.internal.OrderDto;
//import org.example.order.core.infra.messaging.order.message.OrderCloseMessage;
//import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
//import org.example.order.worker.exception.DatabaseExecuteException;
//import org.example.order.worker.exception.WorkerExceptionCode;
//import org.example.order.worker.service.common.KafkaProducerService;
//import org.example.order.worker.service.order.OrderService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
//
///**
// * 파사드 단위 검증:
// * - 성공 경로: service.execute 위임 + 실패 flag 없으면 remote 발행
// * - 실패 경로: 실패 flag → DatabaseExecuteException 발생 & 그룹 DLQ 전송
// */
//class OrderCrudMessageFacadeImplUnitTest {
//
//    private static OrderCrudMessage msg(Operation type, long orderId, boolean failure) {
//        OrderCrudMessage m = mock(OrderCrudMessage.class, RETURNS_DEEP_STUBS);
//        when(m.getMethodType()).thenReturn(type);
//
//        OrderSyncDto local = new OrderSyncDto();
//        local.setOrderId(orderId);
//        local.setFailure(failure);
//
//        OrderDto dto = OrderDto.fromInternal(local);
//        when(m.getDto()).thenReturn(dto);
//
//        return m;
//    }
//
//    @Test
//    @DisplayName("성공: POST 2건 → service 실행 + remote 발행 + DLQ 없음")
//    void execute_success() {
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        OrderService orderService = mock(OrderService.class);
//
//        OrderCrudMessageFacadeImpl sut = new OrderCrudMessageFacadeImpl(producer, orderService);
//
//        var r1 = new ConsumerRecord<>("t", 0, 0, "k1", msg(Operation.POST, 1L, false));
//        var r2 = new ConsumerRecord<>("t", 0, 1, "k2", msg(Operation.POST, 2L, false));
//
//        sut.executeOrderCrud(List.of(r1, r2));
//
//        verify(orderService, times(1)).execute(eq(Operation.POST), anyList());
//        verify(producer, atLeastOnce()).sendToOrderRemote(any(OrderCloseMessage.class));
//        verify(producer, never()).sendToDlq(anyList(), any());
//        verify(producer, never()).sendToDlq(any(DlqMessage.class), any());
//    }
//
//    @Test
//    @DisplayName("실패: 일부 failure=true → DatabaseExecuteException & 그룹 DLQ")
//    void execute_failure_list_goes_dlq() {
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        OrderService orderService = mock(OrderService.class);
//
//        OrderCrudMessageFacadeImpl sut = new OrderCrudMessageFacadeImpl(producer, orderService);
//
//        var r1 = new ConsumerRecord<>("t", 0, 0, "k1", msg(Operation.PUT, 10L, false));
//        var r2 = new ConsumerRecord<>("t", 0, 1, "k2", msg(Operation.PUT, 20L, true));
//
//        assertThatThrownBy(() -> sut.executeOrderCrud(List.of(r1, r2)))
//                .isInstanceOf(DatabaseExecuteException.class)
//                .extracting("code")
//                .isEqualTo(WorkerExceptionCode.MESSAGE_UPDATE_FAILED);
//
//        verify(producer, times(1)).sendToDlq(anyList(), any(DatabaseExecuteException.class));
//        verify(producer, never()).sendToOrderRemote(any());
//        verify(producer, never()).sendToDlq(any(DlqMessage.class), any());
//    }
//}
