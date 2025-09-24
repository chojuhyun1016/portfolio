package org.example.order.core.infra.dynamo.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.dynamo.migration.model.MigrationFile;
import org.example.order.core.infra.dynamo.migration.model.SeedFile;
import org.example.order.core.infra.dynamo.migration.model.TableDef;
import org.example.order.core.infra.dynamo.props.DynamoDbProperties;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DynamoMigrationInitializer
 * ------------------------------------------------------------------------
 * - @Profile("local") 오토컨피그에서만 생성됨
 * - dynamodb.auto-create=true 일 때 동작
 * <p>
 * 동작:
 * 1) 최신 마이그레이션(Vn)만 병합 로드
 * 2) 테이블 미존재 시 CreateTable(+선언된 GSI/LSI)
 * - AttributeDefinitions: "키로 쓰이는" 속성만 포함(테이블 키 + 각 인덱스 키)
 * 3) 테이블 존재 시:
 * - create-missing-gsi=true 이면 누락된 GSI만 UpdateTable로 추가(LSI는 생성 시점에만 가능)
 * 4) 최신 시드(Vn) putItem
 */
@Slf4j
@RequiredArgsConstructor
public class DynamoMigrationInitializer {

    private final DynamoDbClient client;
    private final DynamoDbProperties props;
    private final DynamoMigrationLoader loader;

    @PostConstruct
    public void run() {
        if (!Boolean.TRUE.equals(props.getAutoCreate())) {
            log.info("[DynamoMigration] auto-create=false, skip.");

            return;
        }

        Optional<MigrationFile> migrationOpt = loader.loadLatestMigration(props.getMigrationLocation());
        if (migrationOpt.isEmpty() || migrationOpt.get().getTables().isEmpty()) {
            log.info("[DynamoMigration] No latest migration found. skip.");

            return;
        }

        List<SeedFile> latestSeeds = loader.loadLatestSeeds(props.getSeedLocation()).orElseGet(List::of);
        MigrationFile mf = migrationOpt.get();

        for (TableDef t : mf.getTables()) {
            boolean createdOrUpdated = ensureTableAndIndexes(t);

            // 생성 직후에만 시드 OR 항상 시드: 여기선 "생성된 경우만" 넣도록 유지
            if (createdOrUpdated) {
                latestSeeds.stream()
                        .filter(s -> t.getName().equals(s.getTable()))
                        .forEach(s -> seedTable(t.getName(), s));
            }
        }

        log.info("[DynamoMigration] Latest migration applied.");
    }

    /**
     * 테이블이 없으면 생성(GSI/LSI 포함). 있으면:
     * - create-missing-gsi=true 인 경우, 누락된 GSI만 UpdateTable로 추가
     * 반환값: true이면 "생성됨 또는 인덱스 보강됨"(시드 진행), false이면 아무것도 안 함
     */
    private boolean ensureTableAndIndexes(TableDef t) {
        final String tableName = t.getName();

        if (!exists(tableName)) {
            createTableWithIndexes(t);
            return true;
        }

        // 테이블이 이미 있으면 GSI 보강(옵션)
        boolean gsiUpdated = false;
        if (Boolean.TRUE.equals(props.getCreateMissingGsi()) && t.getGlobalSecondaryIndexes() != null) {
            gsiUpdated = createMissingGsis(tableName, t.getGlobalSecondaryIndexes());
        }

        if (gsiUpdated) {
            log.info("[DynamoMigration] table={} missing GSIs created.", tableName);
        } else {
            log.info("[DynamoMigration] table={} already exists. nothing to do.", tableName);
        }

        return gsiUpdated;
    }

    private void createTableWithIndexes(TableDef t) {
        final String tableName = t.getName();

        // --- 1) 키 스키마(테이블) ---
        List<KeySchemaElement> tableKey = new ArrayList<>();
        tableKey.add(KeySchemaElement.builder()
                .attributeName(t.getHashKey())
                .keyType(KeyType.HASH)
                .build());

        if (hasText(t.getRangeKey())) {
            tableKey.add(KeySchemaElement.builder()
                    .attributeName(t.getRangeKey())
                    .keyType(KeyType.RANGE)
                    .build());
        }

        // --- 2) 선언된 인덱스 수집 ---
        List<GlobalSecondaryIndex> gsis = new ArrayList<>();
        if (t.getGlobalSecondaryIndexes() != null) {
            for (TableDef.GsiDef g : t.getGlobalSecondaryIndexes()) {
                gsis.add(buildGsi(g));
            }
        }

        List<LocalSecondaryIndex> lsis = new ArrayList<>();

        if (t.getLocalSecondaryIndexes() != null && !t.getLocalSecondaryIndexes().isEmpty()) {
            if (!hasText(t.getRangeKey())) {
                throw new IllegalStateException("LSI requires table rangeKey. table=" + tableName);
            }

            for (TableDef.LsiDef l : t.getLocalSecondaryIndexes()) {
                lsis.add(buildLsi(t.getHashKey(), l));
            }
        }

        // --- 3) AttributeDefinitions: "모든 키에 쓰이는" 속성만 포함 ---
        Set<String> keyNames = new LinkedHashSet<>();
        keyNames.add(t.getHashKey());

        if (hasText(t.getRangeKey())) {
            keyNames.add(t.getRangeKey());
        }

        gsis.forEach(g -> g.keySchema().forEach(k -> keyNames.add(k.attributeName())));
        lsis.forEach(l -> l.keySchema().forEach(k -> keyNames.add(k.attributeName())));

        Map<String, String> typeMap = Optional.ofNullable(t.getAttributes())
                .orElseGet(List::of).stream()
                .collect(Collectors.toMap(TableDef.AttributeDef::getName, TableDef.AttributeDef::getType, (a, b) -> a));

        List<AttributeDefinition> attrDefs = keyNames.stream()
                .map(name -> AttributeDefinition.builder()
                        .attributeName(name)
                        .attributeType(toScalar(typeMap.getOrDefault(name, "S")))
                        .build())
                .toList();

        // --- 4) CreateTable ---
        CreateTableRequest.Builder req = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(tableKey)
                .attributeDefinitions(attrDefs)
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits((long) t.getReadCapacity())
                        .writeCapacityUnits((long) t.getWriteCapacity())
                        .build());

        if (!gsis.isEmpty()) {
            req.globalSecondaryIndexes(gsis);
        }

        if (!lsis.isEmpty()) {
            req.localSecondaryIndexes(lsis);
        }

        try {
            client.createTable(req.build());

            log.info("[DynamoMigration] Created table={} (GSI={}, LSI={})", tableName, gsis.size(), lsis.size());

            waitActive(tableName);
        } catch (ResourceInUseException e) {
            log.info("[DynamoMigration] table={} already created concurrently.", tableName);
        } catch (Exception e) {
            throw new IllegalStateException("createTable failed: " + tableName, e);
        }
    }

    /**
     * 누락된 GSI만 UpdateTable로 추가 (LSI는 불가)
     */
    private boolean createMissingGsis(String tableName, List<TableDef.GsiDef> wantGsis) {
        // 현재 존재하는 GSI 목록 조회
        DescribeTableResponse desc = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
        Set<String> existing = Optional.ofNullable(desc.table().globalSecondaryIndexes())
                .orElseGet(List::of).stream().map(GlobalSecondaryIndexDescription::indexName).collect(Collectors.toSet());

        List<GlobalSecondaryIndexUpdate> updates = new ArrayList<>();

        for (TableDef.GsiDef g : wantGsis) {
            if (!existing.contains(g.getName())) {
                updates.add(GlobalSecondaryIndexUpdate.builder()
                        .create(CreateGlobalSecondaryIndexAction.builder()
                                .indexName(g.getName())
                                .keySchema(buildGsiKey(g))
                                .projection(buildProjection(g.getProjection(), g.getNonKeyAttributes()))
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(Optional.ofNullable(g.getReadCapacity()).orElse(5).longValue())
                                        .writeCapacityUnits(Optional.ofNullable(g.getWriteCapacity()).orElse(5).longValue())
                                        .build())
                                .build())
                        .build());
            }
        }

        if (updates.isEmpty()) {
            return false;
        }

        // UpdateTable 호출
        UpdateTableRequest req = UpdateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(collectAttrDefsForGsis(wantGsis))
                .globalSecondaryIndexUpdates(updates)
                .build();

        client.updateTable(req);

        log.info("[DynamoMigration] updateTable(create GSI) table={} count={}", tableName, updates.size());

        waitActive(tableName);

        return true;
    }

    /* ================== Builders & Utils ================== */
    private List<KeySchemaElement> buildGsiKey(TableDef.GsiDef g) {
        List<KeySchemaElement> key = new ArrayList<>();
        key.add(KeySchemaElement.builder().attributeName(g.getHashKey()).keyType(KeyType.HASH).build());

        if (hasText(g.getRangeKey())) {
            key.add(KeySchemaElement.builder().attributeName(g.getRangeKey()).keyType(KeyType.RANGE).build());
        }

        return key;
    }

    private Projection buildProjection(String p, List<String> nonKeys) {
        Projection.Builder proj = Projection.builder().projectionType(toProjection(p));

        if ("INCLUDE".equalsIgnoreCase(p) && nonKeys != null && !nonKeys.isEmpty()) {
            proj.nonKeyAttributes(nonKeys);
        }

        return proj.build();
    }

    private GlobalSecondaryIndex buildGsi(TableDef.GsiDef g) {
        return GlobalSecondaryIndex.builder()
                .indexName(g.getName())
                .keySchema(buildGsiKey(g))
                .projection(buildProjection(g.getProjection(), g.getNonKeyAttributes()))
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(Optional.ofNullable(g.getReadCapacity()).orElse(5).longValue())
                        .writeCapacityUnits(Optional.ofNullable(g.getWriteCapacity()).orElse(5).longValue())
                        .build())
                .build();
    }

    private LocalSecondaryIndex buildLsi(String tableHashKey, TableDef.LsiDef l) {
        List<KeySchemaElement> key = new ArrayList<>();
        key.add(KeySchemaElement.builder().attributeName(tableHashKey).keyType(KeyType.HASH).build());
        key.add(KeySchemaElement.builder().attributeName(l.getRangeKey()).keyType(KeyType.RANGE).build());

        return LocalSecondaryIndex.builder()
                .indexName(l.getName())
                .keySchema(key)
                .projection(buildProjection(l.getProjection(), l.getNonKeyAttributes()))
                .build();
    }

    private List<AttributeDefinition> collectAttrDefsForGsis(List<TableDef.GsiDef> gsis) {
        // GSI 키들만 타입 생성
        Set<String> names = new LinkedHashSet<>();

        for (TableDef.GsiDef g : gsis) {
            names.add(g.getHashKey());

            if (hasText(g.getRangeKey())) {
                names.add(g.getRangeKey());
            }
        }

        // 타입 정보는 마이그 파일의 attributes에서 찾아 기본 S
        // (UpdateTable 시점에는 테이블이 이미 존재하므로 실제 데이터 타입은 중요치 않고, "키 타입 명시"만 필요)
        List<AttributeDefinition> defs = new ArrayList<>();

        for (String n : names) {
            defs.add(AttributeDefinition.builder().attributeName(n).attributeType(ScalarAttributeType.S).build());
        }

        return defs;
    }

    private void waitActive(String tableName) {
        DescribeTableRequest dtr = DescribeTableRequest.builder().tableName(tableName).build();
        WaiterResponse<DescribeTableResponse> waiter = client.waiter().waitUntilTableExists(dtr);
        waiter.matched().response().ifPresent(r ->
                log.info("[DynamoMigration] table={} status={}", tableName, r.table().tableStatus()));
    }

    private boolean exists(String tableName) {
        try {
            client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());

            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private void seedTable(String tableName, SeedFile seed) {
        if (seed.getItems() == null || seed.getItems().isEmpty()) {
            log.info("[DynamoSeed] no items. table={}", tableName);

            return;
        }

        int ok = 0;

        for (Map<String, Object> item : seed.getItems()) {
            Map<String, AttributeValue> av = toAttributeValues(item);

            try {
                client.putItem(PutItemRequest.builder().tableName(tableName).item(av).build());
                ok++;
            } catch (Exception e) {
                log.warn("[DynamoSeed] putItem failed. table={} itemKey={} cause={}",
                        tableName, item.get("id"), e.toString());
            }
        }

        log.info("[DynamoSeed] seeded table={} count={}", tableName, ok);
    }

    private static Map<String, AttributeValue> toAttributeValues(Map<String, Object> item) {
        Map<String, AttributeValue> map = new LinkedHashMap<>();

        for (Map.Entry<String, Object> e : item.entrySet()) {
            AttributeValue v = toAV(e.getValue());

            if (v != null) {
                map.put(e.getKey(), v);
            }
        }

        return map;
    }

    private static AttributeValue toAV(Object v) {
        if (v == null) {
            return null;
        }

        if (v instanceof Number n) {
            return AttributeValue.builder().n(String.valueOf(n)).build();
        }

        if (v instanceof Boolean b) {
            return AttributeValue.builder().s(String.valueOf(b)).build();
        }

        if (v instanceof String s) {
            return AttributeValue.builder().s(s).build();
        }

        if (v instanceof List<?> list) {
            List<AttributeValue> lv = list.stream().map(DynamoMigrationInitializer::toAV).filter(Objects::nonNull).toList();

            return AttributeValue.builder().l(lv).build();
        }

        if (v instanceof Map<?, ?> m) {
            Map<String, AttributeValue> mv = new LinkedHashMap<>();

            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }

                AttributeValue child = toAV(e.getValue());

                if (child != null) {
                    mv.put(String.valueOf(e.getKey()), child);
                }
            }

            return AttributeValue.builder().m(mv).build();
        }

        return AttributeValue.builder().s(String.valueOf(v)).build();
    }

    private static ScalarAttributeType toScalar(String t) {
        if ("N".equalsIgnoreCase(t)) {
            return ScalarAttributeType.N;
        }

        if ("B".equalsIgnoreCase(t)) {
            return ScalarAttributeType.B;
        }

        return ScalarAttributeType.S;
    }

    private static ProjectionType toProjection(String p) {
        if ("KEYS_ONLY".equalsIgnoreCase(p)) {
            return ProjectionType.KEYS_ONLY;
        }

        if ("INCLUDE".equalsIgnoreCase(p)) {
            return ProjectionType.INCLUDE;
        }

        return ProjectionType.ALL;
    }

    private static boolean hasText(String v) {
        return v != null && !v.isBlank();
    }
}
