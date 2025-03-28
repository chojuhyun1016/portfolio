package org.example.order.api.master.service.common;

import org.example.order.core.application.message.OrderRemoteMessage;

public interface KafkaProducerService {
    void sendToOrder(OrderRemoteMessage message);
}
