package org.example.order.core.application.order.dto.command;

import org.example.order.contract.shared.op.Operation;

public record OrderCommand(
        Long orderId,
        Operation operation
) {
}
