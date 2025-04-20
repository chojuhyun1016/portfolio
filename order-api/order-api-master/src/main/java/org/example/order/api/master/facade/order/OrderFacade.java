package org.example.order.api.master.facade.order;

import org.example.order.core.application.order.query.OrderCrudDto;
import org.example.order.core.application.order.event.OrderRemoteMessageDto;

public interface OrderFacade {
    OrderCrudDto fetchById(Long orderId);
    void sendOrderMessage(OrderRemoteMessageDto dto);
}
