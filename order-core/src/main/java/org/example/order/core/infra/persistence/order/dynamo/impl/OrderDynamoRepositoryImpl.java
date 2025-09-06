package org.example.order.core.infra.persistence.order.dynamo.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.model.OrderDynamoQueryOptions;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

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
 */
@Slf4j
public class OrderDynamoRepositoryImpl implements OrderDynamoRepository {

    private final DynamoDbTable<OrderDynamoEntity> table;
    private final String userIdIndexName;

    private final int defaultQueryLimit;
    private final boolean consistentReadGet;
    private final boolean allowScanFallback;

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
        this(enhancedClient, tableName, userIdIndexName, 1000, false, true);
    }

    public OrderDynamoRepositoryImpl(
            DynamoDbEnhancedClient enhancedClient,
            String tableName,
            String userIdIndexName,
            Integer defaultQueryLimit,
            boolean consistentReadGet,
            boolean allowScanFallback
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrderDynamoEntity.class));
        this.userIdIndexName = userIdIndexName;
        this.defaultQueryLimit = (defaultQueryLimit == null || defaultQueryLimit <= 0) ? 1000 : defaultQueryLimit;
        this.consistentReadGet = consistentReadGet;
        this.allowScanFallback = allowScanFallback;
    }

    @PostConstruct
    public void init() {
        log.info("OrderDynamoRepository initialized. table={}, userIdIndexName={}, limit={}, consistentGet={}, allowScanFallback={}",
                table.tableName(), userIdIndexName, defaultQueryLimit, consistentReadGet, allowScanFallback);
    }

    @Override
    public void save(OrderDynamoEntity entity) {
        table.putItem(entity);
        if (log.isDebugEnabled()) {
            log.debug("Saved OrderDynamoEntity: {}", entity);
        }
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

        GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(id).build())
                .consistentRead(opt.consistentReadGet)
                .build();

        OrderDynamoEntity item = table.getItem(req);

        if (log.isDebugEnabled()) {
            log.debug("dynamo_get op=findById id={} consistentRead={} hit={}",
                    id, opt.consistentReadGet, item != null);
        }

        return Optional.ofNullable(item);
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

        if (log.isDebugEnabled()) {
            log.debug("dynamo_scan op=findAll limit={} count={}", opt.limit, result.size());
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
                    List<OrderDynamoEntity> pageItems = page.items();

                    if (pageItems != null) {
                        for (OrderDynamoEntity it : pageItems) {
                            list.add(it);

                            if (list.size() >= opt.limit) {
                                break;
                            }
                        }
                    }

                    if (list.size() >= opt.limit) break;
                }

                if (log.isDebugEnabled()) {
                    log.debug("dynamo_gsi_query op=findByUserId userId={} index={} limit={} count={}",
                            userId, userIdIndexName, opt.limit, list.size());
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

        if (log.isDebugEnabled()) {
            log.debug("dynamo_scan_fallback op=findByUserId userId={} limit={} count={}",
                    userId, opt.limit, result.size());
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
        table.deleteItem(r -> r.key(k -> k.partitionValue(id)));

        if (log.isDebugEnabled()) {
            log.debug("dynamo_delete op=deleteById id={}", id);
        }
    }
}
