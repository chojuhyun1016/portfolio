package org.example.order.core.repository;

import org.example.order.core.application.dto.OrderLocalDto;
import org.example.order.core.application.vo.OrderVo;
import org.example.order.core.domain.OrderEntity;

import java.util.List;

public interface CustomOrderRepository {
    OrderVo fetchByOrderId(Long orderId);
    void bulkInsert(List<OrderEntity> entities);
    void bulkUpdate(List<OrderLocalDto> dtoList);
}
