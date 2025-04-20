package org.example.order.api.master.service.order;

import org.example.order.core.application.order.event.OrderRemoteMessageDto;
import org.example.order.core.application.order.vo.OrderVo;

public interface OrderService {
    OrderVo fetchByIds(Long orderId);
    void sendMessage(OrderRemoteMessageDto dto);
}
