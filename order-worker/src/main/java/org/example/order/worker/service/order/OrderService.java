package org.example.order.worker.service.order;

import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

import java.util.List;

public interface OrderService {
    void execute(Operation operation, List<LocalOrderSync> messages);
}
