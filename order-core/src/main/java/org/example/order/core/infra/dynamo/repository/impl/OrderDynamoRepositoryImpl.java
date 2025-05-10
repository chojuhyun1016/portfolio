package org.example.order.core.infra.dynamo.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Order DynamoDB 리포지토리 구현 (V2 Enhanced Client)
 *
 * - Infra Layer: 외부 DynamoDB 연동만 담당
 * - 비즈니스 로직 없이 순수 Infra 구현
 */
@Slf4j
@Repository
public class OrderDynamoRepositoryImpl implements OrderDynamoRepository {

    private final DynamoDbTable<OrderDynamoEntity> table;

    public OrderDynamoRepositoryImpl(DynamoDbTable<OrderDynamoEntity> table) {
        this.table = table;
    }

    @Override
    public void save(OrderDynamoEntity entity) {
        table.putItem(entity);
        log.debug("Saved OrderDynamoEntity: {}", entity);
    }

    @Override
    public Optional<OrderDynamoEntity> findById(String id) {
        OrderDynamoEntity item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
        log.debug("Fetched OrderDynamoEntity by id={}: {}", id, item);

        return Optional.ofNullable(item);
    }

    @Override
    public List<OrderDynamoEntity> findAll() {
        List<OrderDynamoEntity> items = table.scan()
                .items()
                .stream()
                .collect(Collectors.toList());
        log.debug("Fetched all OrderDynamoEntity, total: {}", items.size());

        return items;
    }

    @Override
    public List<OrderDynamoEntity> findByUserId(Long userId) {
        List<OrderDynamoEntity> result = table.scan()
                .items()
                .stream()
                .filter(item -> userId.equals(item.getUserId()))
                .collect(Collectors.toList());
        log.debug("Fetched OrderDynamoEntity by userId={}, count: {}", userId, result.size());

        return result;
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(r -> r.key(k -> k.partitionValue(id)));
        log.debug("Deleted OrderDynamoEntity with id={}", id);
    }
}
