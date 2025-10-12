package org.example.order.core.application.order.dto.command;

import org.example.order.contract.order.messaging.type.MessageMethodType;

public record LocalOrderCommand(
        Long orderId,
        MessageMethodType methodType
) {
}
