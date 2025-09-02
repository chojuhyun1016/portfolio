package org.example.order.worker.facade.order.impl;

import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.common.OrderWebClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Facade 흐름 검증:
 * - 외부 API 조회 → DTO 변환 → CRUD 발행
 * - 실패 시 단건 DLQ 전송
 */
class OrderApiMessageFacadeImplTest {

    @Test
    @DisplayName("성공: API 조회 후 CRUD 발행, DLQ 미호출")
    void requestApi_success() {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        OrderWebClientService web = mock(OrderWebClientService.class);
        OrderApiMessageFacadeImpl facade = new OrderApiMessageFacadeImpl(producer, web);

        OrderApiMessage msg = mock(OrderApiMessage.class);
        when(msg.getId()).thenReturn(123L);
        when(msg.getPublishedTimestamp()).thenReturn(System.currentTimeMillis());

        LocalOrderDto local = mock(LocalOrderDto.class);
        when(local.getOrderId()).thenReturn(123L);

        OrderDto dto = OrderDto.fromInternal(local);
        when(web.findOrderListByOrderId(123L)).thenReturn(dto);

        facade.requestApi(msg);

        verify(web, times(1)).findOrderListByOrderId(123L);
        verify(producer, times(1)).sendToOrderCrud(any(OrderCrudMessage.class));
        verify(producer, never()).sendToDlq(any(DlqMessage.class), any(Exception.class));
        verify(producer, never()).sendToDlq(anyList(), any(Exception.class));
    }

    @Test
    @DisplayName("실패: API 예외 → 단건 DLQ 전송")
    void requestApi_fail_then_dlq() {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        OrderWebClientService web = mock(OrderWebClientService.class);
        OrderApiMessageFacadeImpl facade = new OrderApiMessageFacadeImpl(producer, web);

        OrderApiMessage msg = mock(OrderApiMessage.class);
        when(msg.getId()).thenReturn(999L);
        when(msg.getPublishedTimestamp()).thenReturn(System.currentTimeMillis());

        when(web.findOrderListByOrderId(999L)).thenThrow(new RuntimeException("api down"));

        try {
            facade.requestApi(msg);
        } catch (Exception ignore) {
        }

        ArgumentCaptor<DlqMessage> dlqCaptor = ArgumentCaptor.forClass(DlqMessage.class);
        ArgumentCaptor<Exception> exCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(producer, times(1)).sendToDlq(dlqCaptor.capture(), exCaptor.capture());

        DlqMessage sent = dlqCaptor.getValue();
        assertThat(sent).isInstanceOf(OrderApiMessage.class);
        assertThat(((OrderApiMessage) sent).getId()).isEqualTo(999L);
        assertThat(exCaptor.getValue()).isInstanceOf(RuntimeException.class);

        verify(producer, never()).sendToDlq(anyList(), any(Exception.class));
    }
}
