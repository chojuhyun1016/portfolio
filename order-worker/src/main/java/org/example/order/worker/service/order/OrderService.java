package org.example.order.worker.service.order;

import org.example.order.common.code.type.MessageMethodType;
import org.example.order.core.application.order.event.message.OrderCrudEvent;

import java.util.List;

public interface OrderService {
    void execute(MessageMethodType methodType, List<OrderCrudEvent> messages);
}
