package org.example.order.api.master.service.common;

import org.example.order.core.application.order.event.message.OrderRemoteEvent;

public interface KafkaProducerService {
    void sendToOrder(OrderRemoteEvent message);
}
