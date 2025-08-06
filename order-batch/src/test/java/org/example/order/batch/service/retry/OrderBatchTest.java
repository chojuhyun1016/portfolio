package org.example.order.batch.service.retry;

import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.impl.OrderDeadLetterServiceImpl;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OrderBatchTest {

    private KafkaProducerService kafkaProducerService;
    private KafkaConsumerProperties kafkaConsumerProperties;
    private KafkaConsumerProperties.KafkaConsumerOption kafkaConsumerOption;

    private OrderDeadLetterServiceImpl orderDeadLetterService;

    @BeforeEach
    void setUp() {
        kafkaProducerService = mock(KafkaProducerService.class);
        kafkaConsumerProperties = mock(KafkaConsumerProperties.class);
        kafkaConsumerOption = mock(KafkaConsumerProperties.KafkaConsumerOption.class);

        when(kafkaConsumerProperties.getOption()).thenReturn(kafkaConsumerOption);
        when(kafkaConsumerOption.getMaxFailCount()).thenReturn(3);

        orderDeadLetterService = new OrderDeadLetterServiceImpl(kafkaProducerService, kafkaConsumerProperties);
    }

    private OrderLocalMessage createValidMessage() {
        OrderLocalMessage message = new OrderLocalMessage();
        message.setId(123L);
        message.setMethodType(MessageMethodType.POST); // ← enum 상수 확인 필요
        message.setPublishedTimestamp(System.currentTimeMillis());
        return message;
    }

    @Test
    void testRetryOrderLocal_discard() {
        // given
        OrderLocalMessage message = createValidMessage();
        for (int i = 0; i < 4; i++) {
            message.increaseFailedCount(); // fail count = 4
        }

        // when
        orderDeadLetterService.retry(message);

        // then
        verify(kafkaProducerService, times(1)).sendToDiscard(message);
        verify(kafkaProducerService, never()).sendToLocal(any());
    }

    @Test
    void testRetryOrderLocal_retry() {
        // given
        OrderLocalMessage message = createValidMessage();
        message.increaseFailedCount(); // fail count = 1

        // when
        orderDeadLetterService.retry(message);

        // then
        verify(kafkaProducerService, times(1)).sendToLocal(message);
        verify(kafkaProducerService, never()).sendToDiscard(any());
    }
}
