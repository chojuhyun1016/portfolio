package org.example.order.core.infra.persistence.order.dynamo.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Order DynamoDB 리포지토리 구현 (V2 Enhanced Client)
 * <p>
 * - Infra Layer: 외부 DynamoDB 연동만 담당
 * - 비즈니스 로직 없이 순수 Infra 구현
 * - @Repository 제거: 설정(Manual/Auto Config)에서만 조건부 등록
 */
@Slf4j
public class OrderDynamoRepositoryImpl implements OrderDynamoRepository {

    private final DynamoDbTable<OrderDynamoEntity> table;

    /**
     * @param enhancedClient DynamoDbEnhancedClient
     * @param tableName      사용할 DynamoDB 테이블명 (설정으로 주입)
     */
    public OrderDynamoRepositoryImpl(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrderDynamoEntity.class));
    }

    @PostConstruct
    public void init() {
        log.info("OrderDynamoRepository initialized with table: {}", table.tableName());
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
