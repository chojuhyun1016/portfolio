package org.example.order.batch.service.retry.impl;

import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.messaging.order.code.DlqOrderType;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderDeadLetterServiceImpl 라우팅/분기 테스트
 * - type별 재발행 경로, 미지원 타입 예외
 * - ObjectMapperUtils 를 static-mock 하여 역직렬화 의존성 제거
 */
class OrderDeadLetterServiceImplTest {

    private static KafkaConsumerProperties mockPropsWithMaxFail(int maxFailCount) {
        KafkaConsumerProperties props = mock(KafkaConsumerProperties.class, RETURNS_DEEP_STUBS);
        when(props.getOption().getMaxFailCount()).thenReturn(maxFailCount);

        return props;
    }

    @Test
    @DisplayName("ORDER_LOCAL → sendToLocal 호출")
    void retry_order_local_should_send_local() {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        KafkaConsumerProperties props = mockPropsWithMaxFail(Integer.MAX_VALUE);
        OrderDeadLetterServiceImpl svc = new OrderDeadLetterServiceImpl(producer, props);

        try (MockedStatic<ObjectMapperUtils> om = mockStatic(ObjectMapperUtils.class)) {
            om.when(() -> ObjectMapperUtils.getFieldValueFromString(anyString(), eq("type"), eq(DlqOrderType.class)))
                    .thenReturn(DlqOrderType.ORDER_LOCAL);

            OrderLocalMessage local = mock(OrderLocalMessage.class);
            when(local.discard(anyInt())).thenReturn(false);
            om.when(() -> ObjectMapperUtils.valueToObject(any(), eq(OrderLocalMessage.class)))
                    .thenReturn(local);

            svc.retry("{\"type\":\"ORDER_LOCAL\"}");

            verify(producer, times(1)).sendToLocal(same(local));
            verify(producer, never()).sendToOrderApi(any(OrderApiMessage.class));
            verify(producer, never()).sendToOrderCrud(any(OrderCrudMessage.class));
        }
    }

    @Test
    @DisplayName("ORDER_API → sendToOrderApi 호출")
    void retry_order_api_should_send_api() {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        KafkaConsumerProperties props = mockPropsWithMaxFail(Integer.MAX_VALUE);
        OrderDeadLetterServiceImpl svc = new OrderDeadLetterServiceImpl(producer, props);

        try (MockedStatic<ObjectMapperUtils> om = mockStatic(ObjectMapperUtils.class)) {
            om.when(() -> ObjectMapperUtils.getFieldValueFromString(anyString(), eq("type"), eq(DlqOrderType.class)))
                    .thenReturn(DlqOrderType.ORDER_API);

            OrderApiMessage api = mock(OrderApiMessage.class);
            when(api.discard(anyInt())).thenReturn(false);
            om.when(() -> ObjectMapperUtils.valueToObject(any(), eq(OrderApiMessage.class)))
                    .thenReturn(api);

            svc.retry("{\"type\":\"ORDER_API\"}");

            verify(producer, times(1)).sendToOrderApi(same(api));
            verify(producer, never()).sendToLocal(any(OrderLocalMessage.class));
            verify(producer, never()).sendToOrderCrud(any(OrderCrudMessage.class));
        }
    }

    @Test
    @DisplayName("ORDER_CRUD → sendToOrderCrud 호출")
    void retry_order_crud_should_send_crud() {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        KafkaConsumerProperties props = mockPropsWithMaxFail(Integer.MAX_VALUE);
        OrderDeadLetterServiceImpl svc = new OrderDeadLetterServiceImpl(producer, props);

        try (MockedStatic<ObjectMapperUtils> om = mockStatic(ObjectMapperUtils.class)) {
            om.when(() -> ObjectMapperUtils.getFieldValueFromString(anyString(), eq("type"), eq(DlqOrderType.class)))
                    .thenReturn(DlqOrderType.ORDER_CRUD);

            OrderCrudMessage crud = mock(OrderCrudMessage.class);
            when(crud.discard(anyInt())).thenReturn(false);
            om.when(() -> ObjectMapperUtils.valueToObject(any(), eq(OrderCrudMessage.class)))
                    .thenReturn(crud);

            svc.retry("{\"type\":\"ORDER_CRUD\"}");

            verify(producer, times(1)).sendToOrderCrud(same(crud));
            verify(producer, never()).sendToLocal(any(OrderLocalMessage.class));
            verify(producer, never()).sendToOrderApi(any(OrderApiMessage.class));
        }
    }

    @Test
    @DisplayName("미등록 타입 → CommonException(UNSUPPORTED_DLQ_TYPE)")
    void retry_unsupported_type_should_throw() {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        KafkaConsumerProperties props = mockPropsWithMaxFail(Integer.MAX_VALUE);
        OrderDeadLetterServiceImpl svc = new OrderDeadLetterServiceImpl(producer, props);

        try (MockedStatic<ObjectMapperUtils> om = mockStatic(ObjectMapperUtils.class)) {
            om.when(() -> ObjectMapperUtils.getFieldValueFromString(anyString(), eq("type"), eq(DlqOrderType.class)))
                    .thenReturn(DlqOrderType.ORDER_REMOTE);

            CommonException ex = assertThrows(CommonException.class, () -> svc.retry("{\"type\":\"ORDER_REMOTE\"}"));
            assertThat(ex.getCode())
                    .as("CommonException.code == UNSUPPORTED_DLQ_TYPE")
                    .isEqualTo(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE.getCode());
        }
    }
}
