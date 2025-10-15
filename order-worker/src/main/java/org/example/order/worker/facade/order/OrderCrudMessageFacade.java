package org.example.order.worker.facade.order;

import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.worker.dto.consumer.OrderCrudConsumerDto;

import java.util.List;

public interface OrderCrudMessageFacade {
    void executeOrderCrud(List<ConsumerEnvelope<OrderCrudConsumerDto>> envelopes);
}
