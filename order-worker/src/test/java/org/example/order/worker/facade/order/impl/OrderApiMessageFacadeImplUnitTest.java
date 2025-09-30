//package org.example.order.worker.facade.order.impl;
//
//import org.example.order.common.core.messaging.message.DlqMessage;
//import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
//import org.example.order.worker.service.common.KafkaProducerService;
//import org.example.order.worker.service.common.WebClientService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
///**
// * 순수 단위 테스트: 스프링/비동기/스케줄러 미사용
// * 현재 구현 기준:
// * - null → DLQ 전송 + IllegalArgumentException
// * - 정상 입력 → (주석으로 인해) 외부 호출/발행 없음
// */
//class OrderApiMessageFacadeImplUnitTest {
//
//    @Test
//    @DisplayName("null 입력 시 DLQ 전송 후 예외")
//    void requestApi_null_then_dlq_and_throw() {
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        WebClientService web = mock(WebClientService.class);
//
//        OrderApiMessageFacadeImpl sut = new OrderApiMessageFacadeImpl(producer, web);
//
//        assertThatThrownBy(() -> sut.requestApi(null))
//                .isInstanceOf(IllegalArgumentException.class);
//
//        verify(producer, times(1)).sendToDlq(any(DlqMessage.class), any(Exception.class));
//        verifyNoMoreInteractions(producer);
//        verifyNoInteractions(web);
//    }
//
//    @Test
//    @DisplayName("정상 입력 시 현재 구현대로 noop")
//    void requestApi_ok_noop_current_impl() {
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        WebClientService web = mock(WebClientService.class);
//
//        OrderApiMessageFacadeImpl sut = new OrderApiMessageFacadeImpl(producer, web);
//
//        OrderApiMessage msg = new OrderApiMessage();
//        sut.requestApi(msg);
//
//        verifyNoInteractions(producer);
//        verifyNoInteractions(web);
//    }
//}
