package org.example.order.core.infra.jpa.repository;

import org.example.order.core.application.order.command.OrderSyncCommand;
import org.example.order.core.application.order.vo.OrderVo;
import org.example.order.core.domain.order.entity.OrderEntity;

import java.util.List;

public interface CustomOrderRepository {
    OrderVo fetchByOrderId(Long orderId);
    void bulkInsert(List<OrderEntity> entities);
    void bulkUpdate(List<OrderSyncCommand> dtoList);
}
