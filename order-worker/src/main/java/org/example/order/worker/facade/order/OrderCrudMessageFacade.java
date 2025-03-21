package org.example.order.worker.facade.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

public interface OrderCrudMessageFacade {
    void executeOrderCrud(List<ConsumerRecord<String, Object>> records);
}
