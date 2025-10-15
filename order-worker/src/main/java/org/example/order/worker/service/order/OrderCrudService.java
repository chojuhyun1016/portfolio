package org.example.order.worker.service.order;

import org.example.order.core.application.order.dto.internal.OrderSyncDto;
import org.example.order.domain.order.entity.OrderEntity;

import java.util.List;

public interface OrderCrudService {
    List<OrderEntity> bulkInsert(List<OrderSyncDto> dtoList);

    void bulkUpdate(List<OrderSyncDto> dtoList);

    void deleteAll(List<OrderSyncDto> dtoList);
}
