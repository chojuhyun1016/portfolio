package org.example.order.core.infra.dynamo.repository;

import org.example.order.domain.order.entity.OrderDynamoEntity;

import java.util.List;
import java.util.Optional;

/**
 * DynamoDB 리포지토리 (V2 Enhanced Client용)
 */
public interface OrderDynamoRepository {

    void save(OrderDynamoEntity entity);

    Optional<OrderDynamoEntity> findById(String id);

    List<OrderDynamoEntity> findAll();

    List<OrderDynamoEntity> findByUserId(Long userId);

    void deleteById(String id);
}
