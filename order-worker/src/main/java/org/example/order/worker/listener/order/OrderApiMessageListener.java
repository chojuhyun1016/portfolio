package org.example.order.worker.listener.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

public interface OrderApiMessageListener {
    void orderApi(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment);
}
