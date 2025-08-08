package org.example.order.api.master.service.common;

import org.example.order.core.messaging.order.message.OrderLocalMessage;

public interface KafkaProducerService {
    void sendToOrder(OrderLocalMessage message);
}
