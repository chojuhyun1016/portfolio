package org.example.order.worker.listener.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface OrderCrudMessageListener {
    void executeOrderCrud(List<ConsumerRecord<String, OrderCrudMessage>> records, Acknowledgment acknowledgment);
}
