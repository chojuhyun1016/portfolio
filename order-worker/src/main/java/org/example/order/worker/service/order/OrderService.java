package org.example.order.worker.service.order;

import org.example.order.worker.dto.command.OrderCrudBatchCommand;

/**
 * OrderService
 * - 연산별 배치 커맨드를 받아 CRUD 오케스트레이션 수행
 */
public interface OrderService {
    void execute(OrderCrudBatchCommand batch);
}
