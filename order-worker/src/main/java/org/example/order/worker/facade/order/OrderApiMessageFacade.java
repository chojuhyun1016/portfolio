package org.example.order.worker.facade.order;

import org.example.order.core.infra.messaging.order.message.OrderApiMessage;

public interface OrderApiMessageFacade {
    void requestApi(OrderApiMessage message);
}
