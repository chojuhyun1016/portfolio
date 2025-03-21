package org.example.order.worker.listener.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface OrderCrudMessageListener {
    void executeOrderCrud(List<ConsumerRecord<String, Object>> records, Acknowledgment acknowledgment);
}
