package org.example.order.core.infra.persistence.order.dynamo.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final String userIdIndexName;

    /**
     * @param enhancedClient DynamoDbEnhancedClient
     * @param tableName      사용할 DynamoDB 테이블명 (설정으로 주입)
     */
    public OrderDynamoRepositoryImpl(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this(enhancedClient, tableName, null);
    }

    /**
     * [ADD] GSI 인덱스명 주입 오버로드 (테스트/기존 호출은 기본 생성자 사용 가능)
     *
     * @param enhancedClient  DynamoDbEnhancedClient
     * @param tableName       테이블명
     * @param userIdIndexName userId 파티션키를 사용하는 GSI 이름 (예: "idx_user_id"), null이면 scan() fallback
     */
    public OrderDynamoRepositoryImpl(DynamoDbEnhancedClient enhancedClient, String tableName, String userIdIndexName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrderDynamoEntity.class));
        this.userIdIndexName = userIdIndexName;
    }

    @PostConstruct
    public void init() {
        log.info("OrderDynamoRepository initialized with table: {}, userIdIndexName: {}", table.tableName(), userIdIndexName);
    }

    @Override
    public void save(OrderDynamoEntity entity) {
        table.putItem(entity);

        if (log.isDebugEnabled()) {
            log.debug("Saved OrderDynamoEntity: {}", entity);
        }
    }

    @Override
    public Optional<OrderDynamoEntity> findById(String id) {
        OrderDynamoEntity item = table.getItem(r -> r.key(k -> k.partitionValue(id)));

        if (log.isDebugEnabled()) {
            log.debug("Fetched OrderDynamoEntity by id={}: {}", id, item);
        }

        return Optional.ofNullable(item);
    }

    @Override
    public List<OrderDynamoEntity> findAll() {
        List<OrderDynamoEntity> items = new ArrayList<>();
        PageIterable<OrderDynamoEntity> iterable = table.scan();

        for (Page<OrderDynamoEntity> page : iterable) {
            items.addAll(page.items());
        }

        if (log.isDebugEnabled()) {
            log.debug("Fetched all OrderDynamoEntity, total: {}", items.size());
        }

        return items;
    }

    @Override
    public List<OrderDynamoEntity> findByUserId(Long userId) {
        if (userIdIndexName != null && !userIdIndexName.isBlank()) {
            try {
                DynamoDbIndex<OrderDynamoEntity> index = table.index(userIdIndexName);

                List<OrderDynamoEntity> list = new ArrayList<>();
                SdkIterable<Page<OrderDynamoEntity>> pages = index.query(r ->
                        r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(userId)))
                );

                for (Page<OrderDynamoEntity> page : pages) {
                    list.addAll(page.items());
                }

                if (log.isDebugEnabled()) {
                    log.debug("GSI query by userId={}, count={}", userId, list.size());
                }

                return list;
            } catch (Exception e) {
                log.warn("GSI [{}] query failed, fallback to scan. cause={}", userIdIndexName, e.toString());
            }
        }

        List<OrderDynamoEntity> result = new ArrayList<>();
        PageIterable<OrderDynamoEntity> iterable = table.scan();

        for (Page<OrderDynamoEntity> page : iterable) {
            for (OrderDynamoEntity item : page.items()) {
                if (userId != null && userId.equals(item.getUserId())) {
                    result.add(item);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Fetched OrderDynamoEntity by userId={}, count: {}", userId, result.size());
        }
        return result;
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(r -> r.key(k -> k.partitionValue(id)));

        if (log.isDebugEnabled()) {
            log.debug("Deleted OrderDynamoEntity with id={}", id);
        }
    }
}
