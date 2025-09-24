package org.example.order.worker.facade.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;

import java.util.List;

public interface OrderCrudMessageFacade {
    void executeOrderCrud(List<ConsumerRecord<String, OrderCrudMessage>> records);
}
