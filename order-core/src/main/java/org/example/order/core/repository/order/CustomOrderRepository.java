package org.example.order.core.repository.order;

import org.example.order.core.application.dto.order.OrderLocalDto;
import org.example.order.core.application.vo.order.OrderVo;
import org.example.order.core.domain.order.OrderEntity;

import java.util.List;

public interface CustomOrderRepository {
    OrderVo fetchByOrderId(Long orderId);
    void bulkInsert(List<OrderEntity> entities);
    void bulkUpdate(List<OrderLocalDto> dtoList);
}
