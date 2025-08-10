package com.example.order.api.web.facade.order;

import com.example.order.api.web.dto.order.OrderResponse;

public interface OrderFacade {
    OrderResponse findById(Long id);
}
