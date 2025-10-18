package org.example.order.worker.service.order;

import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.domain.order.entity.OrderEntity;

import java.util.List;

public interface OrderCrudService {
    List<OrderEntity> bulkInsert(List<LocalOrderSync> dtoList);

    void bulkUpdate(List<LocalOrderSync> dtoList);

    void deleteAll(List<LocalOrderSync> dtoList);
}
