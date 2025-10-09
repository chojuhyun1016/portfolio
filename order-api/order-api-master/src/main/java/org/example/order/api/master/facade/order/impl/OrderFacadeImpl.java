package org.example.order.api.master.facade.order.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.api.master.mapper.order.OrderRequestMapper;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.springframework.stereotype.Component;

/**
 * 파사드 구현
 * - 메시지 발행/전송
 * - 조회(가공 포함) 위임
 */
@RequiredArgsConstructor
@Component
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final OrderRequestMapper orderRequestMapper;

    @Override
    public void sendOrderMessage(LocalOrderRequest request) {
        LocalOrderCommand command = orderRequestMapper.toCommand(request);
        orderService.sendMessage(command);
    }

    @Override
    public OrderDto findById(Long orderId) {
        return orderService.findById(orderId);
    }
}
