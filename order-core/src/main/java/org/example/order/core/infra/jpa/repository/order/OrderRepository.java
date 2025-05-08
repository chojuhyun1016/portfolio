package org.example.order.core.infra.jpa.repository.order;

import org.example.order.domain.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, CustomOrderRepository {
    void deleteByOrderIdIn(List<Long> orderId);
}
