package org.example.order.worker.listener.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.springframework.kafka.support.Acknowledgment;

public interface OrderApiMessageListener {
    void orderApi(ConsumerRecord<String, OrderApiMessage> record, Acknowledgment acknowledgment);
}
