package org.example.order.api.master.service.order;

import org.example.order.core.application.dto.OrderRemoteMessageDto;
import org.example.order.core.application.vo.OrderVo;

public interface OrderService {
    OrderVo fetchByIds(Long orderId);
    void sendMessage(OrderRemoteMessageDto dto);
}
