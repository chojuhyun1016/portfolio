package org.example.order.worker.facade.order;

import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.worker.dto.consumer.OrderLocalConsumerDto;

public interface OrderLocalMessageFacade {
    void sendOrderApiTopic(ConsumerEnvelope<OrderLocalConsumerDto> envelope);
}
