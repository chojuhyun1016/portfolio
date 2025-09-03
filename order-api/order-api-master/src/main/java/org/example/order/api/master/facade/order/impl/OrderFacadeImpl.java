package org.example.order.api.master.facade.order.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.api.master.mapper.order.OrderRequestMapper;
import org.example.order.api.master.service.order.OrderService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final OrderRequestMapper orderRequestMapper;

    @Override
    public void sendOrderMessage(LocalOrderRequest request) {
        var command = orderRequestMapper.toCommand(request);

        orderService.sendMessage(command);
    }
}
