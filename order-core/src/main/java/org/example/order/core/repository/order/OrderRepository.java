package org.example.order.core.repository.order;

import org.example.order.core.domain.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, CustomOrderRepository {
    void deleteByOrderIdIn(List<Long> orderId);
}
