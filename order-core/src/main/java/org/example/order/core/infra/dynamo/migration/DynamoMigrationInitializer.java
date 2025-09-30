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

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DynamoMigrationInitializer
 * ------------------------------------------------------------------------
 * 목적/범위:
 * - 로컬/Dev 환경에서 DynamoDB 테이블과 인덱스(GSI/LSI)를 마이그레이션 스키마에 맞게 보장한다.
 * - 최신 Vn 스키마/시드만 적용한다(여러 파일이면 병합).
 * <p>
 * 동작 흐름:
 * 1) 최신 마이그레이션 파일 로드
 * 2) 각 테이블에 대해:
 * 2-1) 테이블이 없으면 생성(선언된 GSI/LSI 포함)
 * 2-2) 테이블이 있으면 "안전 변경"만 자동 적용
 * - 누락된 GSI 자동 추가
 * 2-3) 스키마 드리프트 감지/조정(reconcile): [Local/Dev 전용]
 * - 안전 변경: 누락된 GSI 추가(중복 방지), (옵션) 마이그레이션에 없는 GSI 삭제
 * - 위험 변경: 테이블 키 변경, LSI 변경, GSI 키/프로젝션/키타입 변경
 * · 기본값: DRY-RUN (로그만 남기고 실제 변경 없음)
 * · 옵션 allowDestructive=true: 파괴적 재생성(drop & recreate)
 * · (옵션) copyData=true 이면 임시 테이블로 복사 후 교체 (maxItemCount 상한)
 * 3) 테이블이 "신규 생성"된 경우에만 시드 putItem 수행
 * <p>
 * 설계 주의:
 * - DynamoDB는 스키마리스이므로 컬럼(일반 속성)은 강제/변경 대상이 아니다.
 * → 비교/변경은 키 스키마(HASH/RANGE)와 인덱스(GSI/LSI)만을 대상으로 한다.
 * - 처리량 자동 조정은 제외(테스트용).
 * <p>
 * 추가 보강:
 * - 모든 테이블/인덱스 키 속성은 attributes에 타입(S/N/B)을 반드시 선언하도록 강제(미선언 시 기동 실패).
 * - 드리프트 비교에 “키 타입” 불일치도 포함.
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
            String table = t.getName();

            // (0) 키 타입 선언 여부 사전 검증(필수)
            validateKeyTypesDeclared(t);

            // 1) 생성 + 누락 GSI 자동 보강(안전 변경)
            TableEnsureResult result = ensureTableAndIndexes(t);

            // 2) 드리프트 감지/조정 (Local/Dev 한정)
            if (Boolean.TRUE.equals(props.getSchemaReconcile().getEnabled()) && exists(table)) {
                reconcileSchema(t);
            }

            // 3) 신규 생성일 때만 시드
            if (result.created()) {
                latestSeeds.stream()
                        .filter(s -> table.equals(s.getTable()))
                        .forEach(s -> seedTable(table, s));
            }
        }

        log.info("[DynamoMigration] Latest migration applied.");
    }

    /* ===================== 1) 생성 & 누락 GSI 추가(안전 변경) ===================== */

    private TableEnsureResult ensureTableAndIndexes(TableDef t) {
        final String tableName = t.getName();

        if (!exists(tableName)) {
            createTableWithIndexes(t);

            return new TableEnsureResult(true, false);
        }

        boolean gsiUpdated = false;

        if (Boolean.TRUE.equals(props.getCreateMissingGsi()) && t.getGlobalSecondaryIndexes() != null) {
            gsiUpdated = createMissingGsis(tableName, t);
        }

        if (gsiUpdated) {
            log.info("[DynamoMigration] table={} missing GSIs created.", tableName);
        } else {
            log.info("[DynamoMigration] table={} already exists. nothing to do.", tableName);
        }

        return new TableEnsureResult(false, gsiUpdated);
    }

    private void createTableWithIndexes(TableDef t) {
        final String tableName = t.getName();

        // 필수: 키 타입 선언 검증
        validateKeyTypesDeclared(t);

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

        List<GlobalSecondaryIndex> gsis = new ArrayList<>();

        if (t.getGlobalSecondaryIndexes() != null) {
            for (TableDef.GsiDef g : t.getGlobalSecondaryIndexes()) {
                gsis.add(buildGsi(t, g));
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

        // AttributeDefinitions: 모든 키의 union + 타입은 attributes에서 "반드시" 가져옴
        Map<String, String> typeMap = attributesTypeMap(t);
        Set<String> keyNames = new LinkedHashSet<>();
        keyNames.add(t.getHashKey());

        if (hasText(t.getRangeKey())) {
            keyNames.add(t.getRangeKey());
        }

        gsis.forEach(g -> g.keySchema().forEach(k -> keyNames.add(k.attributeName())));
        lsis.forEach(l -> l.keySchema().forEach(k -> keyNames.add(k.attributeName())));

        List<AttributeDefinition> attrDefs = keyNames.stream()
                .map(name -> AttributeDefinition.builder()
                        .attributeName(name)
                        .attributeType(scalarOfRequired(typeMap, name, tableName))
                        .build())
                .toList();

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

    private boolean createMissingGsis(String tableName, TableDef t) {
        // 필수: 키 타입 선언 검증
        validateKeyTypesDeclared(t);

        DescribeTableResponse desc = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
        Set<String> existing = Optional.ofNullable(desc.table().globalSecondaryIndexes())
                .orElseGet(List::of).stream()
                .map(GlobalSecondaryIndexDescription::indexName)
                .collect(Collectors.toSet());

        boolean created = false;

        for (TableDef.GsiDef g : Optional.ofNullable(t.getGlobalSecondaryIndexes()).orElseGet(List::of)) {
            if (existing.contains(g.getName())) {
                continue;
            }

            createOneGsiWithRetry(tableName, t, g);
            waitUntilGsiActive(tableName, g.getName());
            created = true;
            existing.add(g.getName());
        }

        return created;
    }

    private void createOneGsiWithRetry(String tableName, TableDef t, TableDef.GsiDef g) {
        int attempt = 0, maxAttempts = 10;

        while (true) {
            try {
                UpdateTableRequest.Builder req = UpdateTableRequest.builder()
                        .tableName(tableName)
                        .attributeDefinitions(collectAttrDefsForGsi(t, g));

                CreateGlobalSecondaryIndexAction.Builder create = CreateGlobalSecondaryIndexAction.builder()
                        .indexName(g.getName())
                        .keySchema(buildGsiKey(g))
                        .projection(buildProjection(g.getProjection(), g.getNonKeyAttributes()))
                        .provisionedThroughput(ProvisionedThroughput.builder()
                                .readCapacityUnits((long) Optional.ofNullable(g.getReadCapacity()).orElse(5))
                                .writeCapacityUnits((long) Optional.ofNullable(g.getWriteCapacity()).orElse(5))
                                .build());

                req.globalSecondaryIndexUpdates(GlobalSecondaryIndexUpdate.builder()
                        .create(create.build())
                        .build());

                client.updateTable(req.build());

                log.info("[DynamoMigration] updateTable(create GSI) table={} index={}", tableName, g.getName());

                return;
            } catch (LimitExceededException e) {
                attempt++;
                long backoffMs = backoffMillis(attempt);

                log.warn("[DynamoMigration] LimitExceeded creating GSI. table={} index={} attempt={} waitMs={}",
                        tableName, g.getName(), attempt, backoffMs);

                sleep(backoffMs);

                if (attempt >= maxAttempts) {
                    throw new IllegalStateException("Exceeded retries creating GSI " + g.getName() + " on " + tableName, e);
                }
            } catch (ResourceInUseException e) {
                long waitMs = 2000;

                log.warn("[DynamoMigration] ResourceInUse creating GSI. table={} index={} waitMs={}",
                        tableName, g.getName(), waitMs);

                sleep(waitMs);
            }
        }
    }

    /* ===================== 2) 스키마 드리프트 감지 & 조정 ===================== */

    private void reconcileSchema(TableDef want) {
        String table = want.getName();
        DescribeTableResponse dtr = client.describeTable(DescribeTableRequest.builder().tableName(table).build());
        TableDescription cur = dtr.table();

        Drift drift = diff(cur, want);

        if (!drift.isEmpty()) {
            log.warn("[SchemaDrift] table={} detected drift: {}", table, drift);
        } else {
            log.info("[SchemaDrift] table={} no drift", table);

            return;
        }

        // 안전 변경: 누락 GSI 추가
        if (!drift.missingGsis.isEmpty()) {
            log.info("[SchemaDrift] table={} missingGsis={}", table, drift.missingGsis);

            for (TableDef.GsiDef g : drift.missingGsis) {
                createOneGsiWithRetry(table, want, g);
                waitUntilGsiActive(table, g.getName());
            }
        }

        // (옵션) 불필요 GSI 삭제
        if (props.getSchemaReconcile().getDeleteExtraGsi() && !drift.extraGsis.isEmpty()) {
            log.warn("[SchemaDrift] table={} delete extra GSIs={}", table, drift.extraGsis);

            for (String gsiName : drift.extraGsis) {
                client.updateTable(UpdateTableRequest.builder()
                        .tableName(table)
                        .globalSecondaryIndexUpdates(GlobalSecondaryIndexUpdate.builder()
                                .delete(DeleteGlobalSecondaryIndexAction.builder()
                                        .indexName(gsiName)
                                        .build())
                                .build())
                        .build());

                waitUntilNoGsi(table, gsiName);
            }
        }

        // 위험 변경: DRY-RUN or 파괴적 재생성
        boolean needRebuild =
                drift.tableKeyChanged || drift.lsiChanged
                        || !drift.gsiKeyOrProjectionMismatches.isEmpty()
                        || drift.keyTypeMismatches; // 🔸 키 타입 불일치 추가

        if (needRebuild) {
            if (Boolean.TRUE.equals(props.getSchemaReconcile().getDryRun())) {
                log.warn("[SchemaDrift][DRY-RUN] table={} requires REBUILD due to {}. Skipped (dryRun=true).",
                        table, drift.reason());

                return;
            }

            if (!Boolean.TRUE.equals(props.getSchemaReconcile().getAllowDestructive())) {
                log.warn("[SchemaDrift] table={} requires REBUILD but allowDestructive=false. Skipped.", table);

                return;
            }

            rebuildTableFromMigration(want, cur);
        }
    }

    private void rebuildTableFromMigration(TableDef want, TableDescription cur) {
        String table = want.getName();
        String tmp = table + "__rebuild";

        log.warn("[Rebuild] START table={} -> tmp={}", table, tmp);

        // 1) 임시 테이블(목표 스키마)
        TableDef clone = new TableDef();
        clone.setName(tmp);
        clone.setHashKey(want.getHashKey());
        clone.setRangeKey(want.getRangeKey());
        clone.setReadCapacity(want.getReadCapacity());
        clone.setWriteCapacity(want.getWriteCapacity());
        clone.setAttributes(want.getAttributes());
        clone.setGlobalSecondaryIndexes(want.getGlobalSecondaryIndexes());
        clone.setLocalSecondaryIndexes(want.getLocalSecondaryIndexes());

        createTableWithIndexes(clone);
        waitActive(tmp);

        // 2) 데이터 복사(옵션, 상한)
        if (Boolean.TRUE.equals(props.getSchemaReconcile().getCopyData())) {
            long count = approximateItemCount(cur);
            long limit = Optional.ofNullable(props.getSchemaReconcile().getMaxItemCount()).orElse(10_000L);

            if (count > limit) {
                throw new IllegalStateException("[Rebuild] item count " + count + " exceeds limit " + limit + " (copyData=true)");
            }

            copyAllItems(table, tmp);
        }

        // 3) 기존 삭제 → 4) 새로 생성
        client.deleteTable(DeleteTableRequest.builder().tableName(table).build());
        waitDeleted(table);

        createTableWithIndexes(want);
        waitActive(table);

        // 5) (복사한 경우) tmp → 새 테이블로 복사 후 tmp 삭제
        if (Boolean.TRUE.equals(props.getSchemaReconcile().getCopyData())) {
            copyAllItems(tmp, table);
            client.deleteTable(DeleteTableRequest.builder().tableName(tmp).build());
            waitDeleted(tmp);
        }

        log.warn("[Rebuild] DONE table={}", table);
    }

    /* ===================== 2-a) DRIFT 계산 (키 타입 포함) ===================== */

    private Drift diff(TableDescription cur, TableDef want) {
        Drift d = new Drift();

        // 현재 테이블의 AttributeDefinitions -> 타입 맵
        Map<String, ScalarAttributeType> curTypeMap =
                Optional.ofNullable(cur.attributeDefinitions()).orElseGet(List::of).stream()
                        .collect(Collectors.toMap(AttributeDefinition::attributeName, AttributeDefinition::attributeType));

        Map<String, String> wantTypeMapStr = attributesTypeMap(want);

        // 테이블 키 비교 + 타입 비교
        String curHash = cur.keySchema().stream().filter(k -> k.keyType() == KeyType.HASH).findFirst().map(KeySchemaElement::attributeName).orElse(null);
        String curRange = cur.keySchema().stream().filter(k -> k.keyType() == KeyType.RANGE).findFirst().map(KeySchemaElement::attributeName).orElse(null);
        d.tableKeyChanged = !Objects.equals(curHash, want.getHashKey()) || !Objects.equals(curRange, nullIfBlank(want.getRangeKey()));

        // 키 타입 체크
        d.keyTypeMismatches |= !Objects.equals(curTypeMap.get(want.getHashKey()), toScalar(wantTypeMapStr.get(want.getHashKey())));

        if (hasText(want.getRangeKey())) {
            d.keyTypeMismatches |= !Objects.equals(curTypeMap.get(want.getRangeKey()), toScalar(wantTypeMapStr.get(want.getRangeKey())));
        }

        // LSI 비교 (이름/키/Projection) — 타입은 테이블 RANGE 키에 종속이므로 별도 비교 X

        Map<String, LocalSecondaryIndexDescription> curLsi = Optional.ofNullable(cur.localSecondaryIndexes()).orElseGet(List::of)
                .stream().collect(Collectors.toMap(LocalSecondaryIndexDescription::indexName, x -> x));
        Map<String, TableDef.LsiDef> wantLsi = Optional.ofNullable(want.getLocalSecondaryIndexes()).orElseGet(List::of)
                .stream().collect(Collectors.toMap(TableDef.LsiDef::getName, x -> x));

        if (!curLsi.keySet().equals(wantLsi.keySet())) {
            d.lsiChanged = true;
        } else {
            for (String name : wantLsi.keySet()) {
                LocalSecondaryIndexDescription a = curLsi.get(name);
                TableDef.LsiDef b = wantLsi.get(name);
                String aRange = a.keySchema().stream().filter(k -> k.keyType() == KeyType.RANGE).findFirst().map(KeySchemaElement::attributeName).orElse(null);

                boolean projMismatch = !Objects.equals(projType(a.projection()), b.getProjection())
                        || !equalSet(Optional.ofNullable(a.projection().nonKeyAttributes()).orElseGet(List::of),
                        Optional.ofNullable(b.getNonKeyAttributes()).orElseGet(List::of));

                if (!Objects.equals(aRange, b.getRangeKey()) || projMismatch) {
                    d.lsiChanged = true;

                    break;
                }
            }
        }

        // GSI 비교 + GSI 키 타입도 체크
        Map<String, GlobalSecondaryIndexDescription> curGsi = Optional.ofNullable(cur.globalSecondaryIndexes()).orElseGet(List::of)
                .stream().collect(Collectors.toMap(GlobalSecondaryIndexDescription::indexName, x -> x));
        Map<String, TableDef.GsiDef> wantGsi = Optional.ofNullable(want.getGlobalSecondaryIndexes()).orElseGet(List::of)
                .stream().collect(Collectors.toMap(TableDef.GsiDef::getName, x -> x));

        for (String name : wantGsi.keySet()) {
            if (!curGsi.containsKey(name)) {
                d.missingGsis.add(wantGsi.get(name));
            }
        }

        for (String name : curGsi.keySet()) {
            if (!wantGsi.containsKey(name)) {
                d.extraGsis.add(name);
            }
        }

        for (String name : wantGsi.keySet()) {
            if (!curGsi.containsKey(name)) {
                continue;
            }

            GlobalSecondaryIndexDescription a = curGsi.get(name);
            TableDef.GsiDef b = wantGsi.get(name);

            String aHash = a.keySchema().stream().filter(k -> k.keyType() == KeyType.HASH).findFirst().map(KeySchemaElement::attributeName).orElse(null);
            String aRange = a.keySchema().stream().filter(k -> k.keyType() == KeyType.RANGE).findFirst().map(KeySchemaElement::attributeName).orElse(null);

            boolean keyMismatch = !Objects.equals(aHash, b.getHashKey()) || !Objects.equals(aRange, nullIfBlank(b.getRangeKey()));
            boolean projMismatch = !Objects.equals(projType(a.projection()), b.getProjection())
                    || !equalSet(Optional.ofNullable(a.projection().nonKeyAttributes()).orElseGet(List::of),
                    Optional.ofNullable(b.getNonKeyAttributes()).orElseGet(List::of));

            if (keyMismatch || projMismatch) {
                d.gsiKeyOrProjectionMismatches.add(name);
            }

            // GSI 키 타입 비교 (테이블 AttributeDefinitions에 포함된 타입 기준)
            d.keyTypeMismatches |= !Objects.equals(curTypeMap.get(b.getHashKey()), toScalar(wantTypeMapStr.get(b.getHashKey())));

            if (hasText(b.getRangeKey())) {
                d.keyTypeMismatches |= !Objects.equals(curTypeMap.get(b.getRangeKey()), toScalar(wantTypeMapStr.get(b.getRangeKey())));
            }
        }

        return d;
    }

    /* ===================== 2-b) REBUILD & COPY 유틸 ===================== */

    private long approximateItemCount(TableDescription cur) {
        return Optional.ofNullable(cur.itemCount()).orElse(0L);
    }

    private void copyAllItems(String src, String dst) {
        log.warn("[Rebuild] copy start: {} -> {}", src, dst);

        Map<String, AttributeValue> exclusiveStartKey = null;
        int batch = 0;
        List<WriteRequest> buffer = new ArrayList<>(25);

        while (true) {
            ScanResponse scan = client.scan(ScanRequest.builder()
                    .tableName(src)
                    .limit(1000)
                    .exclusiveStartKey(exclusiveStartKey)
                    .build());

            for (Map<String, AttributeValue> item : scan.items()) {
                buffer.add(WriteRequest.builder()
                        .putRequest(PutRequest.builder().item(item).build())
                        .build());

                if (buffer.size() == 25) {
                    batchWrite(dst, buffer);
                    batch++;
                    buffer.clear();
                }
            }

            exclusiveStartKey = scan.lastEvaluatedKey();

            if (exclusiveStartKey == null || exclusiveStartKey.isEmpty()) {
                break;
            }
        }

        if (!buffer.isEmpty()) {
            batchWrite(dst, buffer);
            batch++;
        }

        log.warn("[Rebuild] copy done: {} -> {} (batches={})", src, dst, batch);
    }

    private void batchWrite(String dst, List<WriteRequest> writes) {
        Map<String, List<WriteRequest>> req = new HashMap<>();
        req.put(dst, new ArrayList<>(writes));

        while (true) {
            BatchWriteItemResponse resp = client.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(req)
                    .build());

            Map<String, List<WriteRequest>> unprocessed = resp.unprocessedItems();

            if (unprocessed == null || unprocessed.isEmpty()) {
                break;
            }

            sleep(500);
            req = unprocessed;
        }
    }

    /* ===================== 공통 유틸 ===================== */

    private static String nullIfBlank(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private static void validateKeyTypesDeclared(TableDef t) {
        Map<String, String> typeMap = attributesTypeMap(t);
        List<String> missing = new ArrayList<>();

        // 테이블 키
        if (!typeMap.containsKey(t.getHashKey())) {
            missing.add(t.getHashKey());
        }

        if (hasText(t.getRangeKey()) && !typeMap.containsKey(t.getRangeKey())) {
            missing.add(t.getRangeKey());
        }

        // GSI 키
        for (TableDef.GsiDef g : Optional.ofNullable(t.getGlobalSecondaryIndexes()).orElseGet(List::of)) {
            if (!typeMap.containsKey(g.getHashKey())) {
                missing.add(g.getHashKey());
            }

            if (hasText(g.getRangeKey()) && !typeMap.containsKey(g.getRangeKey())) {
                missing.add(g.getRangeKey());
            }
        }

        // LSI 키(테이블 HASH + LSI RANGE) — 테이블 HASH는 앞에서 이미 검사됨
        for (TableDef.LsiDef l : Optional.ofNullable(t.getLocalSecondaryIndexes()).orElseGet(List::of)) {
            if (!typeMap.containsKey(l.getRangeKey())) {
                missing.add(l.getRangeKey());
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("[DynamoMigration] Key attribute type missing in attributes: " + missing + " (table=" + t.getName() + ")");
        }
    }

    private void waitUntilGsiActive(String tableName, String indexName) {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();

        while (System.currentTimeMillis() < deadline) {
            DescribeTableResponse resp = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());

            boolean tableActive = resp.table().tableStatus() == TableStatus.ACTIVE;
            boolean indexActive = Optional.ofNullable(resp.table().globalSecondaryIndexes())
                    .orElseGet(List::of).stream()
                    .anyMatch(i -> indexName.equals(i.indexName()) && i.indexStatus() == IndexStatus.ACTIVE);

            if (tableActive && indexActive) {
                log.info("[DynamoMigration] table={} index={} status=ACTIVE", tableName, indexName);
                return;
            }

            sleep(2000);
        }

        throw new IllegalStateException("GSI " + indexName + " on " + tableName + " not ACTIVE within timeout");
    }

    private void waitUntilNoGsi(String tableName, String indexName) {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();

        while (System.currentTimeMillis() < deadline) {
            DescribeTableResponse resp = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            boolean exists = Optional.ofNullable(resp.table().globalSecondaryIndexes())
                    .orElseGet(List::of).stream()
                    .anyMatch(i -> indexName.equals(i.indexName()));

            if (!exists) {
                return;
            }

            sleep(1500);
        }

        throw new IllegalStateException("GSI " + indexName + " not deleted within timeout for table " + tableName);
    }

    private void waitActive(String tableName) {
        DescribeTableRequest dtr = DescribeTableRequest.builder().tableName(tableName).build();
        WaiterResponse<DescribeTableResponse> waiter = client.waiter().waitUntilTableExists(dtr);
        waiter.matched().response().ifPresent(r ->
                log.info("[DynamoMigration] table={} status={}", tableName, r.table().tableStatus()));
    }

    private void waitDeleted(String tableName) {
        DescribeTableRequest dtr = DescribeTableRequest.builder().tableName(tableName).build();
        client.waiter().waitUntilTableNotExists(dtr);

        log.info("[DynamoMigration] table={} deleted", tableName);
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

    private static Map<String, String> attributesTypeMap(TableDef t) {
        return Optional.ofNullable(t.getAttributes())
                .orElseGet(List::of).stream()
                .collect(Collectors.toMap(TableDef.AttributeDef::getName, TableDef.AttributeDef::getType, (a, b) -> a, LinkedHashMap::new));
    }

    private static ScalarAttributeType scalarOfRequired(Map<String, String> typeMap, String name, String tableName) {
        String t = typeMap.get(name);

        if (t == null) {
            throw new IllegalStateException("[DynamoMigration] Missing type for key attribute '" + name + "' (table=" + tableName + ")");
        }

        return toScalar(t);
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
            return AttributeValue.builder().bool(b).build();
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

    private static Projection buildProjection(String p, List<String> nonKeys) {
        Projection.Builder proj = Projection.builder().projectionType(toProjection(p));

        if ("INCLUDE".equalsIgnoreCase(p) && nonKeys != null && !nonKeys.isEmpty()) {
            proj.nonKeyAttributes(nonKeys);
        }

        return proj.build();
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

    private static List<KeySchemaElement> buildGsiKey(TableDef.GsiDef g) {
        List<KeySchemaElement> key = new ArrayList<>();
        key.add(KeySchemaElement.builder().attributeName(g.getHashKey()).keyType(KeyType.HASH).build());

        if (hasText(g.getRangeKey())) {
            key.add(KeySchemaElement.builder().attributeName(g.getRangeKey()).keyType(KeyType.RANGE).build());
        }

        return key;
    }

    private GlobalSecondaryIndex buildGsi(TableDef t, TableDef.GsiDef g) {
        return GlobalSecondaryIndex.builder()
                .indexName(g.getName())
                .keySchema(buildGsiKey(g))
                .projection(buildProjection(g.getProjection(), g.getNonKeyAttributes()))
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits((long) Optional.ofNullable(g.getReadCapacity()).orElse(5))
                        .writeCapacityUnits((long) Optional.ofNullable(g.getWriteCapacity()).orElse(5))
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

    private List<AttributeDefinition> collectAttrDefsForGsi(TableDef t, TableDef.GsiDef g) {
        Map<String, String> typeMap = attributesTypeMap(t);
        List<AttributeDefinition> defs = new ArrayList<>();

        defs.add(AttributeDefinition.builder()
                .attributeName(g.getHashKey())
                .attributeType(scalarOfRequired(typeMap, g.getHashKey(), t.getName()))
                .build());

        if (hasText(g.getRangeKey())) {
            defs.add(AttributeDefinition.builder()
                    .attributeName(g.getRangeKey())
                    .attributeType(scalarOfRequired(typeMap, g.getRangeKey(), t.getName()))
                    .build());
        }

        return defs;
    }

    private static boolean hasText(String v) {
        return v != null && !v.isBlank();
    }

    private static String projType(Projection p) {
        return p == null || p.projectionType() == null ? "ALL" : p.projectionType().toString();
    }

    private static boolean equalSet(List<String> a, List<String> b) {
        return new HashSet<>(a).equals(new HashSet<>(b));
    }

    private static long backoffMillis(int attempt) {
        long base = 1000L;
        int capped = Math.min(attempt, 5);
        long ms = base * (1L << capped);
        return Math.min(ms, 15000L);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /* ===================== 내부 상태 객체 ===================== */

    private record TableEnsureResult(boolean created, boolean gsiUpdated) {
    }

    private static class Drift {
        boolean tableKeyChanged = false;
        boolean lsiChanged = false;
        boolean keyTypeMismatches = false; // 🔸 키 타입 불일치
        final List<TableDef.GsiDef> missingGsis = new ArrayList<>();
        final List<String> extraGsis = new ArrayList<>();
        final List<String> gsiKeyOrProjectionMismatches = new ArrayList<>();

        boolean isEmpty() {
            return !tableKeyChanged && !lsiChanged && !keyTypeMismatches
                    && missingGsis.isEmpty() && extraGsis.isEmpty()
                    && gsiKeyOrProjectionMismatches.isEmpty();
        }

        String reason() {
            List<String> r = new ArrayList<>();
            if (tableKeyChanged) {
                r.add("TABLE_KEY_CHANGED");
            }

            if (lsiChanged) {
                r.add("LSI_CHANGED");
            }

            if (keyTypeMismatches) {
                r.add("KEY_TYPE_MISMATCH");
            }

            if (!gsiKeyOrProjectionMismatches.isEmpty()) {
                r.add("GSI_KEY/PROJECTION_CHANGED=" + gsiKeyOrProjectionMismatches);
            }

            return String.join(",", r);
        }

        @Override
        public String toString() {
            return "Drift{" +
                    "tableKeyChanged=" + tableKeyChanged +
                    ", lsiChanged=" + lsiChanged +
                    ", keyTypeMismatches=" + keyTypeMismatches +
                    ", missingGsis=" + names(missingGsis) +
                    ", extraGsis=" + extraGsis +
                    ", gsiKeyOrProjectionMismatches=" + gsiKeyOrProjectionMismatches +
                    '}';
        }

        private List<String> names(List<TableDef.GsiDef> xs) {
            return xs.stream().map(TableDef.GsiDef::getName).toList();
        }
    }
}
