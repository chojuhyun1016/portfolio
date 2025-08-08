package org.example.order.api.master.service.order;

import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.OrderDto;

public interface OrderService {
    OrderDto findById(Long id);

    void sendMessage(LocalOrderCommand command);
}
