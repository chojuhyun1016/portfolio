package org.example.order.worker.dto.command;

import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

import java.util.List;

/**
 * CREATE 배치 커맨드 (dto 계층)
 */
public record OrderCreateBatchCommand(List<LocalOrderSync> items) implements OrderCrudBatchCommand {
    @Override
    public Operation operation() {
        return Operation.CREATE;
    }
}
