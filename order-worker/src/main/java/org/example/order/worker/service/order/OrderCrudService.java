package org.example.order.worker.service.order;

import org.example.order.core.application.order.command.OrderSyncCommand;
import org.example.order.core.domain.OrderEntity;

import java.util.List;

public interface OrderCrudService {
    List<OrderEntity> bulkInsert(List<OrderSyncCommand> dtoList);
    void bulkUpdate(List<OrderSyncCommand> dtoList);
    void deleteAll(List<OrderSyncCommand> dtoList);
}
