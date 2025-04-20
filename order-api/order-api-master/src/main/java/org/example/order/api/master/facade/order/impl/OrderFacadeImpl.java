package org.example.order.api.master.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.core.application.order.query.OrderCrudDto;
import org.example.order.core.application.order.event.OrderRemoteMessageDto;
import org.example.order.core.application.order.vo.OrderVo;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderFacadeImpl implements OrderFacade {
    private final OrderService orderService;

    @Override
    public OrderCrudDto fetchById(Long orderId) {
        OrderVo vo = orderService.fetchByIds(orderId);
        return vo == null ? null : OrderCrudDto.toDto(vo);
    }

    @Override
    public void sendOrderMessage(OrderRemoteMessageDto dto) {
        orderService.sendMessage(dto);
    }
}
