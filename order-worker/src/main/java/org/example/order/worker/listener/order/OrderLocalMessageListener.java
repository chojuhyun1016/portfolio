package org.example.order.worker.listener.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

public interface OrderLocalMessageListener {
    void orderLocal(ConsumerRecord<String, OrderLocalMessage> record, Acknowledgment acknowledgment);
}
