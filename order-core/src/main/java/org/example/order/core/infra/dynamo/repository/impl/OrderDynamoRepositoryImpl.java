package org.example.order.core.infra.dynamo.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.dynamo.repository.OrderDynamoRepository;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB 리포지토리 구현 (V2 Enhanced Client)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderDynamoRepositoryImpl implements OrderDynamoRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private static final String TABLE_NAME = "order_dynamo";

    private DynamoDbTable<OrderDynamoEntity> getTable() {
        return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(OrderDynamoEntity.class));
    }

    @Override
    public void save(OrderDynamoEntity entity) {
        getTable().putItem(entity);
    }

    @Override
    public Optional<OrderDynamoEntity> findById(String id) {
        return Optional.ofNullable(getTable().getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    @Override
    public List<OrderDynamoEntity> findAll() {
        List<OrderDynamoEntity> result = new ArrayList<>();
        getTable().scan().items().forEach(result::add);

        return result;
    }

    @Override
    public List<OrderDynamoEntity> findByUserId(Long userId) {
        // 주의: GSI 또는 스캔 기반 (비효율적일 수 있음) -> 차후 UserIdIndex (GSI) 방식으로 개선
        List<OrderDynamoEntity> result = new ArrayList<>();

        getTable().scan().items().forEach(item -> {
            if (userId.equals(item.getUserId())) {
                result.add(item);
            }
        });

        return result;
    }

    @Override
    public void deleteById(String id) {
        getTable().deleteItem(r -> r.key(k -> k.partitionValue(id)));
    }
}
