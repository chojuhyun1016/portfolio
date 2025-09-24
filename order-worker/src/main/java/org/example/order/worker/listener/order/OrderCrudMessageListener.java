package org.example.order.worker.listener.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;

public interface OrderCrudMessageListener {
    void executeOrderCrud(List<ConsumerRecord<String, OrderCrudMessage>> records, Acknowledgment acknowledgment);
}
