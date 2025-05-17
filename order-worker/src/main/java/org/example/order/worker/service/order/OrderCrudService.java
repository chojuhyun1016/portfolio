package org.example.order.worker.service.order;

import org.example.order.core.application.order.dto.command.OrderSyncCommandDto;
import org.example.order.domain.order.entity.OrderEntity;

import java.util.List;

public interface OrderCrudService {
    List<OrderEntity> bulkInsert(List<OrderSyncCommandDto> dtoList);
    void bulkUpdate(List<OrderSyncCommandDto> dtoList);
    void deleteAll(List<OrderSyncCommandDto> dtoList);
}
