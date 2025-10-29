package org.example.order.api.web.service.common;

import org.example.order.contract.order.messaging.event.OrderLocalMessage;

public interface KafkaProducerService {
    void sendToOrder(OrderLocalMessage message);
}

