package org.example.order.worker.service.order;

import org.example.order.core.application.dto.order.OrderLocalDto;
import org.example.order.core.domain.order.OrderEntity;

import java.util.List;

public interface OrderCrudService {
    List<OrderEntity> bulkInsert(List<OrderLocalDto> dtoList);
    void bulkUpdate(List<OrderLocalDto> dtoList);
    void deleteAll(List<OrderLocalDto> dtoList);
}
