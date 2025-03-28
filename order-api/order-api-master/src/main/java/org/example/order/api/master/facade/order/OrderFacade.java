package org.example.order.api.master.facade.order;

import org.example.order.core.application.dto.OrderCrudDto;
import org.example.order.core.application.dto.OrderRemoteMessageDto;

public interface OrderFacade {
    OrderCrudDto fetchById(Long orderId);
    void sendOrderMessage(OrderRemoteMessageDto dto);
}
