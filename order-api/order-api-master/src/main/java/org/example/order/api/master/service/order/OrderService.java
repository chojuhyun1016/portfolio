package org.example.order.api.master.service.order;

import org.example.order.core.application.order.dto.command.LocalOrderCommand;

public interface OrderService {
    void sendMessage(LocalOrderCommand command);
}
