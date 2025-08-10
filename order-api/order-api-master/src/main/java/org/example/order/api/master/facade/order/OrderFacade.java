package org.example.order.api.master.facade.order;

import org.example.order.api.master.dto.order.LocalOrderRequest;

public interface OrderFacade {
    void sendOrderMessage(LocalOrderRequest request);
}
