package org.example.order.api.master.service.common;

import org.example.order.core.application.message.order.OrderRemoteMessage;

public interface KafkaProducerService {
    void sendToOrder(OrderRemoteMessage message);
}
