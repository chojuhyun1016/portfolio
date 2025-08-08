package org.example.order.core.application.order.dto.command;

import org.example.order.common.core.messaging.code.MessageMethodType;

public record LocalOrderCommand(
        Long orderId,
        MessageMethodType methodType
) {}
