package org.example.order.core.infra.persistence.order.dynamo.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.model.OrderDynamoQueryOptions;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Order DynamoDB 리포지토리 구현 (V2 Enhanced Client)
 * <p>
 * - Infra Layer: 외부 DynamoDB 연동만 담당
 * - 비즈니스 로직 없이 순수 Infra 구현
 * - @Repository 제거: 설정(Manual/Auto Config)에서만 조건부 등록
 * <p>
 * 운영 규약:
 * - 복합키 테이블(PK+SK)에서 단건 조회 시 getItem 금지(PK+SK 모두 알아야 하므로).
 * → PartitionKey(id)만으로 "Query" 수행, 첫 건을 단건으로 취급(접근 패턴 기준).
 * - Scan은 운영 금지(옵션으로만 허용). 다른 접근 패턴은 전용 GSI 설계.
 */
@Slf4j
public class OrderDynamoRepositoryImpl implements OrderDynamoRepository {

    /**
     * BatchWriteItemEnhanced 를 사용하기 위해 EnhancedClient 보관
     */
    private final DynamoDbEnhancedClient enhancedClient;

    private final DynamoDbTable<OrderDynamoEntity> table;
    private final String userIdIndexName;

    private final int defaultQueryLimit;
    private final boolean consistentReadGet;
    private final boolean allowScanFallback;

    public OrderDynamoRepositoryImpl(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this(enhancedClient, tableName, null);
    }

    /**
     * [ADD] GSI 인덱스명 주입 오버로드 (테스트/기존 호출은 기본 생성자 사용 가능)
     *
     * @param enhancedClient  DynamoDbEnhancedClient
     * @param tableName       테이블명
     * @param userIdIndexName userId 파티션키를 사용하는 GSI 이름 (예: "gsi_user_id"), null이면 scan() fallback
     */
    public OrderDynamoRepositoryImpl(DynamoDbEnhancedClient enhancedClient, String tableName, String userIdIndexName) {
        // 운영 기본: limit=1000, consistentRead=false, scanFallback=false
        this(enhancedClient, tableName, userIdIndexName, 1000, false, false);
    }

    public OrderDynamoRepositoryImpl(
            DynamoDbEnhancedClient enhancedClient,
            String tableName,
            String userIdIndexName,
            Integer defaultQueryLimit,
            boolean consistentReadGet,
            boolean allowScanFallback
    ) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrderDynamoEntity.class));
        this.userIdIndexName = userIdIndexName;
        this.defaultQueryLimit = (defaultQueryLimit == null || defaultQueryLimit <= 0) ? 1000 : defaultQueryLimit;
        this.consistentReadGet = consistentReadGet;
        this.allowScanFallback = allowScanFallback;
    }

    @PostConstruct
    public void init() {
        log.info("OrderDynamoRepository initialized. table={}, userIdIndexName={}, limit={}, consistentRead={}, allowScanFallback={}",
                table.tableName(), userIdIndexName, defaultQueryLimit, consistentReadGet, allowScanFallback);
    }

    @Override
    public void save(OrderDynamoEntity entity) {
        table.putItem(entity);
    }

    // === 기본 시그니처(호환) ===
    @Override
    public Optional<OrderDynamoEntity> findById(String id) {
        return findById(id, null);
    }

    @Override
    public List<OrderDynamoEntity> findAll() {
        return findAll(null);
    }

    @Override
    public List<OrderDynamoEntity> findByUserId(Long userId) {
        return findByUserId(userId, null);
    }

    // === 옵션 기반 오버로드 ===
    @Override
    public Optional<OrderDynamoEntity> findById(String id, OrderDynamoQueryOptions options) {
        ResolvedOptions opt = resolve(options);

        PageIterable<OrderDynamoEntity> pages = table.query(QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(id)))
                .limit(1)
                .consistentRead(opt.consistentReadGet) // Base Table Query에 한해 strong read 가능
                .build()
        );

        for (Page<OrderDynamoEntity> page : pages) {
            if (page.items() != null) {
                for (OrderDynamoEntity it : page.items()) {
                    return Optional.ofNullable(it);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<OrderDynamoEntity> findAll(OrderDynamoQueryOptions options) {
        ResolvedOptions opt = resolve(options);

        PageIterable<OrderDynamoEntity> pages = table.scan(
                ScanEnhancedRequest.builder().limit(opt.limit).build()
        );

        List<OrderDynamoEntity> result = new ArrayList<>();
        SdkIterable<OrderDynamoEntity> items = pages.items();

        for (OrderDynamoEntity e : items) {
            result.add(e);

            if (result.size() >= opt.limit) {
                break;
            }
        }

        return result;
    }

    @Override
    public List<OrderDynamoEntity> findByUserId(Long userId, OrderDynamoQueryOptions options) {
        ResolvedOptions opt = resolve(options);

        if (userIdIndexName != null && !userIdIndexName.isBlank()) {
            try {
                DynamoDbIndex<OrderDynamoEntity> index = table.index(userIdIndexName);
                SdkIterable<Page<OrderDynamoEntity>> pages = index.query(r ->
                        r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(userId)))
                                .limit(opt.limit)
                );

                List<OrderDynamoEntity> list = new ArrayList<>();

                for (Page<OrderDynamoEntity> page : pages) {
                    if (page.items() != null) {
                        for (OrderDynamoEntity it : page.items()) {
                            list.add(it);

                            if (list.size() >= opt.limit) {
                                break;
                            }
                        }
                    }

                    if (list.size() >= opt.limit) {
                        break;
                    }
                }

                return list;
            } catch (Exception e) {
                log.warn("GSI [{}] query failed, allowScanFallback={}, cause={}",
                        userIdIndexName, opt.allowScanFallback, e.toString());
            }
        }

        if (!opt.allowScanFallback) {
            log.warn("Scan fallback disabled. op=findByUserId userId={}", userId);

            return Collections.emptyList();
        }

        PageIterable<OrderDynamoEntity> pages = table.scan(
                ScanEnhancedRequest.builder().limit(opt.limit).build()
        );

        List<OrderDynamoEntity> result = new ArrayList<>();

        for (OrderDynamoEntity item : pages.items()) {
            if (userId != null && userId.equals(item.getUserId())) {
                result.add(item);

                if (result.size() >= opt.limit) {
                    break;
                }
            }
        }

        return result;
    }

    private ResolvedOptions resolve(OrderDynamoQueryOptions options) {
        int limit = (options != null && options.getLimit() != null && options.getLimit() > 0)
                ? options.getLimit() : defaultQueryLimit;

        boolean consistent = (options != null && options.getConsistentRead() != null)
                ? options.getConsistentRead() : consistentReadGet;

        boolean fallback = (options != null && options.getAllowScanFallback() != null)
                ? options.getAllowScanFallback() : allowScanFallback;

        return new ResolvedOptions(limit, consistent, fallback);
    }

    private record ResolvedOptions(int limit, boolean consistentReadGet, boolean allowScanFallback) {
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("Composite PK table requires orderNumber. Use deleteByIdAndOrderNumber(id, orderNumber). id=" + id);
    }

    /**
     * PK+SK 삭제 (테이블이 Sort Key를 **요구**하므로 폴백 없이 정확히 삭제)
     */
    @Override
    public void deleteByIdAndOrderNumber(String id, String orderNumber) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id(partition key) is required");
        }

        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("orderNumber(sort key) is required for deletion");
        }

        table.deleteItem(r -> r.key(k -> k.partitionValue(id).sortValue(orderNumber)));
    }

    /**
     * 동일 PK에 속한 모든 아이템 삭제 (PK만 알고 있을 때 사용)
     * - 내부적으로 Query 후 배치 삭제(BatchWriteItemEnhanced) 25개 단위
     * - 미처리 항목은 지수 백오프로 재시도
     */
    @Override
    public void deleteAllByPartition(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id(partition key) is required");
        }

        final int CHUNK = 25;
        int totalDeleted = 0;
        List<Key> buffer = new ArrayList<>(CHUNK);

        PageIterable<OrderDynamoEntity> pages = table.query(r ->
                r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(id)))
        );

        for (OrderDynamoEntity item : pages.items()) {
            String sk = item.getOrderNumber();

            if (sk == null || sk.isBlank()) {
                log.warn("Skip delete: found item without sort key. id={} item={}", id, item);

                continue;
            }

            buffer.add(Key.builder().partitionValue(id).sortValue(sk).build());

            if (buffer.size() == CHUNK) {
                totalDeleted += batchDeleteKeys(buffer);
                buffer.clear();
            }
        }

        if (!buffer.isEmpty()) {
            totalDeleted += batchDeleteKeys(buffer);
        }
    }

    /**
     * BatchWriteItemEnhanced 로 25개 키를 한 번에 삭제
     * - 미처리(unprocessed) 항목은 최대 5회 지수 백오프 재시도
     */
    private int batchDeleteKeys(List<Key> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        List<Key> remaining = new ArrayList<>(keys);
        int deleted = 0;

        int attempt = 0;
        int maxAttempts = 5;
        long backoffMillis = 100L;

        while (!remaining.isEmpty() && attempt < maxAttempts) {
            attempt++;

            WriteBatch.Builder<OrderDynamoEntity> wb = WriteBatch.builder(OrderDynamoEntity.class)
                    .mappedTableResource(table);

            for (Key k : remaining) {
                wb.addDeleteItem(k);
            }

            BatchWriteItemEnhancedRequest req = BatchWriteItemEnhancedRequest.builder()
                    .addWriteBatch(wb.build())
                    .build();

            try {
                BatchWriteResult result = enhancedClient.batchWriteItem(req);

                List<Key> unprocessed = result.unprocessedDeleteItemsForTable(table);
                int processed = remaining.size() - (unprocessed == null ? 0 : unprocessed.size());
                deleted += processed;

                if (unprocessed == null || unprocessed.isEmpty()) {
                    remaining.clear();
                } else {
                    remaining = new ArrayList<>(unprocessed);

                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();

                        break;
                    }

                    backoffMillis = Math.min((long) (backoffMillis * 2), Duration.ofSeconds(3).toMillis());
                }
            } catch (ProvisionedThroughputExceededException pte) {
                log.warn("Batch delete throttled. attempt={} size={} cause={}", attempt, remaining.size(), pte.toString());

                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();

                    break;
                }
                backoffMillis = Math.min((long) (backoffMillis * 2), Duration.ofSeconds(3).toMillis());
            } catch (Throwable t) {
                log.warn("Batch delete failed. attempt={} size={} cause={}", attempt, remaining.size(), t.toString());

                break;
            }
        }

        if (!remaining.isEmpty()) {
            log.warn("Batch delete finished with unprocessed items. remaining={}", remaining.size());
        }

        return deleted;
    }
}
