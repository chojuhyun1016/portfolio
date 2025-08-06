package org.example.order.batch.service.retry;

import org.example.order.batch.service.retry.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.service.retry.impl.OrderDeadLetterServiceImpl;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
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

    @Test
    void testRetryOrderLocal_discard() {
        // given
        OrderLocalMessage message = new OrderLocalMessage(); // 기본 생성자 사용
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
        OrderLocalMessage message = new OrderLocalMessage();
        message.increaseFailedCount(); // fail count = 1

        // when
        orderDeadLetterService.retry(message);

        // then
        verify(kafkaProducerService, times(1)).sendToLocal(message);
        verify(kafkaProducerService, never()).sendToDiscard(any());
    }
}
