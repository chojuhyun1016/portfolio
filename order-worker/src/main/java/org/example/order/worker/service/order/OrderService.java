package org.example.order.worker.service.order;

import org.example.order.common.code.MessageMethodType;
import org.example.order.core.application.event.OrderCrudEvent;

import java.util.List;

public interface OrderService {
    void execute(MessageMethodType methodType, List<OrderCrudEvent> messages);
}
