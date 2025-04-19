package org.example.order.api.master.service.common;

import org.example.order.core.application.event.OrderRemoteEvent;

public interface KafkaProducerService {
    void sendToOrder(OrderRemoteEvent message);
}
