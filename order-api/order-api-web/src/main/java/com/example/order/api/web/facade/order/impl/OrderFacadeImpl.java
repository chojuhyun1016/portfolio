package com.example.order.api.web.facade.order.impl;

import com.example.order.api.web.dto.order.OrderResponse;
import com.example.order.api.web.facade.order.OrderFacade;
import com.example.order.api.web.mapper.order.OrderResponseMapper;
import com.example.order.api.web.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final OrderResponseMapper orderResponseMapper;

    @Override
    public OrderResponse findById(Long id) {
        var dto = orderService.findById(id);

        return orderResponseMapper.toResponse(dto);
    }
}
