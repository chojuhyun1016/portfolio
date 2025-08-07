package org.example.order.batch.service.retry;

import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.impl.OrderDeadLetterServiceImpl;
import org.example.order.client.kafka.config.properties.KafkaConsumerProperties;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OrderDeadLetterServiceTest {

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

    private OrderLocalMessage createValidMessage(int failCount) {
        OrderLocalMessage message = new OrderLocalMessage();
        message.setId(123L);
        message.setMethodType(MessageMethodType.POST);
        message.setPublishedTimestamp(System.currentTimeMillis());

        for (int i = 0; i < failCount; i++) {
            message.increaseFailedCount();
        }

        return message;
    }

    @Test
    void shouldSendToDiscard_whenFailCountExceedsThreshold() {
        // given
        OrderLocalMessage message = createValidMessage(4); // maxFailCount = 3

        // when
        orderDeadLetterService.retry(message);

        // then
        verify(kafkaProducerService).sendToDiscard(message);
        verify(kafkaProducerService, never()).sendToLocal(any());
    }

    @Test
    void shouldRetry_whenFailCountIsWithinThreshold() {
        // given
        OrderLocalMessage message = createValidMessage(2);

        // when
        orderDeadLetterService.retry(message);

        // then
        verify(kafkaProducerService).sendToLocal(message);
        verify(kafkaProducerService, never()).sendToDiscard(any());
    }
}
