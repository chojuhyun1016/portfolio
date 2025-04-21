package org.example.order.core.infra.dynamo.repository;

import org.example.order.core.infra.dynamo.model.OrderDynamoEntity;

import java.util.List;
import java.util.Optional;

public interface OrderDynamoRepository {
    void save(OrderDynamoEntity entity);
    Optional<OrderDynamoEntity> findById(String id);
    List<OrderDynamoEntity> findAll();
    List<OrderDynamoEntity> findByUserId(Long userId);
}
