package org.example.order.domain.order.repository;

import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.model.OrderDynamoQueryOptions;

import java.util.List;
import java.util.Optional;

/**
 * Order DynamoDB 리포지토리 인터페이스 (V2 Enhanced Client용)
 * <p>
 * - Domain Layer에 위치 (비즈니스 로직 계층)
 * - 구현은 Infra Layer에서 담당
 */
public interface OrderDynamoRepository {

    void save(OrderDynamoEntity entity);

    Optional<OrderDynamoEntity> findById(String id);

    List<OrderDynamoEntity> findAll();

    List<OrderDynamoEntity> findByUserId(Long userId);

    void deleteById(String id);

    default Optional<OrderDynamoEntity> findById(String id, OrderDynamoQueryOptions options) {
        return findById(id);
    }

    default List<OrderDynamoEntity> findAll(OrderDynamoQueryOptions options) {
        return findAll();
    }

    default List<OrderDynamoEntity> findByUserId(Long userId, OrderDynamoQueryOptions options) {
        return findByUserId(userId);
    }
}
