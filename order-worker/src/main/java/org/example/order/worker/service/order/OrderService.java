package org.example.order.worker.service.order;

import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.messaging.order.message.OrderCrudEvent;

import java.util.List;

public interface OrderService {
    void execute(MessageMethodType methodType, List<OrderCrudEvent> messages);
}
