package org.example.order.api.master.facade.order.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.dto.order.OrderResponse;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.api.master.mapper.order.OrderRequestMapper;
import org.example.order.api.master.mapper.order.OrderResponseMapper;
import org.example.order.api.master.service.order.OrderService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final OrderRequestMapper orderRequestMapper;
    private final OrderResponseMapper orderResponseMapper;

    @Override
    public OrderResponse findById(Long id) {
        var dto = orderService.findById(id);
        return orderResponseMapper.toResponse(dto);
    }

    @Override
    public void sendOrderMessage(LocalOrderRequest request) {
        var command = orderRequestMapper.toCommand(request);
        orderService.sendMessage(command);
    }
}
