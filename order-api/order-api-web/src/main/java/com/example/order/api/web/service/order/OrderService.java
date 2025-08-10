package com.example.order.api.web.service.order;

import org.example.order.core.application.order.dto.internal.OrderDto;

public interface OrderService {
    OrderDto findById(Long id);
}
