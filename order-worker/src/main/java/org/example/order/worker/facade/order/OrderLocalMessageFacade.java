package org.example.order.worker.facade.order;

import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;

public interface OrderLocalMessageFacade {
    void sendOrderApiTopic(OrderLocalMessage message);
}
