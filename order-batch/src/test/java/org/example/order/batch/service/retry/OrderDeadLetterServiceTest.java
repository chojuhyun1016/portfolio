//package org.example.order.batch.service.retry;
//
//import org.example.order.batch.service.common.KafkaProducerService;
//import org.example.order.batch.service.retry.impl.OrderDeadLetterServiceImpl;
//import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.example.order.common.support.json.ObjectMapperUtils;
//import org.example.order.core.messaging.order.message.OrderLocalMessage;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.MockedStatic;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//class OrderDeadLetterServiceTest {
//
//    private KafkaProducerService kafkaProducerService;
//    private KafkaConsumerProperties kafkaConsumerProperties;
//    private KafkaConsumerProperties.KafkaConsumerOption kafkaConsumerOption;
//
//    private OrderDeadLetterServiceImpl orderDeadLetterService;
//
//    @BeforeEach
//    void setUp() {
//        kafkaProducerService = mock(KafkaProducerService.class);
//        kafkaConsumerProperties = mock(KafkaConsumerProperties.class);
//        kafkaConsumerOption = mock(KafkaConsumerProperties.KafkaConsumerOption.class);
//
//        when(kafkaConsumerProperties.getOption()).thenReturn(kafkaConsumerOption);
//        when(kafkaConsumerOption.getMaxFailCount()).thenReturn(3);
//
//        orderDeadLetterService = new OrderDeadLetterServiceImpl(kafkaProducerService, kafkaConsumerProperties);
//    }
//
//    private OrderLocalMessage createValidMessage(int failCount) {
//        OrderLocalMessage message = new OrderLocalMessage();
//        message.setId(123L);
//        message.setMethodType(MessageMethodType.POST);
//        message.setPublishedTimestamp(System.currentTimeMillis());
//        for (int i = 0; i < failCount; i++) {
//            message.increaseFailedCount();
//        }
//        return message;
//    }
//
//    @Test
//    void shouldSendToDiscard_whenFailCountExceedsThreshold() {
//        // given: maxFailCount = 3, 메시지 실패 4회
//        OrderLocalMessage message = createValidMessage(4);
//
//        try (MockedStatic<ObjectMapperUtils> omu = mockStatic(ObjectMapperUtils.class)) {
//            // getFieldValueFromString(String json, String field, Class<T> type) → 3개 인자
//            omu.when(() -> ObjectMapperUtils.getFieldValueFromString(anyString(), eq("type"), eq(String.class)))
//                    .thenReturn("ORDER_LOCAL");
//
//            // valueToObject(Object value, Class<T> type) → 2개 인자
//            omu.when(() -> ObjectMapperUtils.valueToObject(any(), eq(OrderLocalMessage.class)))
//                    .thenReturn(message);
//
//            // when
//            orderDeadLetterService.retry(message);
//
//            // then
//            verify(kafkaProducerService).sendToDiscard(message);
//            verify(kafkaProducerService, never()).sendToLocal(any());
//        }
//    }
//
//    @Test
//    void shouldRetry_whenFailCountIsWithinThreshold() {
//        // given: maxFailCount = 3, 메시지 실패 2회
//        OrderLocalMessage message = createValidMessage(2);
//
//        try (MockedStatic<ObjectMapperUtils> omu = mockStatic(ObjectMapperUtils.class)) {
//            omu.when(() -> ObjectMapperUtils.getFieldValueFromString(anyString(), eq("type"), eq(String.class)))
//                    .thenReturn("ORDER_LOCAL");
//            omu.when(() -> ObjectMapperUtils.valueToObject(any(), eq(OrderLocalMessage.class)))
//                    .thenReturn(message);
//
//            // when
//            orderDeadLetterService.retry(message);
//
//            // then
//            verify(kafkaProducerService).sendToLocal(message);
//            verify(kafkaProducerService, never()).sendToDiscard(any());
//        }
//    }
//}
