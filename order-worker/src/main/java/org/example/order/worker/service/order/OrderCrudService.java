package org.example.order.worker.service.order;

import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.domain.order.entity.OrderEntity;

import java.util.List;

public interface OrderCrudService {
    List<OrderEntity> bulkInsert(List<LocalOrderDto> dtoList);
    void bulkUpdate(List<LocalOrderDto> dtoList);
    void deleteAll(List<LocalOrderDto> dtoList);
}
