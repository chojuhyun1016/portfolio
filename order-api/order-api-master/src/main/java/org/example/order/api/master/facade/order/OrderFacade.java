package org.example.order.api.master.facade.order;

import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.dto.order.OrderResponse;

public interface OrderFacade {
    OrderResponse findById(Long id);

    void sendOrderMessage(LocalOrderRequest request);
}
