package org.example.order.core.infra.dynamo;

/**
 * DynamoDB 통합테스트.
 *
 * - LocalStackDynamoSupport 를 상속받아 LocalStack(DynamoDB) 컨테이너를 공유한다.
 * - 누락되었던 TABLE 상수 및 ensureTable 로직을 본 파일에 구현하여
 *   컴파일 에러를 제거한다.
 * - Enhanced Client 의 Table#createTable() 를 사용하여 엔티티 정의에 맞는 테이블을 생성한다.
 *
 * 주의: integrationTest 소스셋 전용. main/test 소스셋에는 영향 없음.
 */

import org.example.order.core.infra.dynamo.support.LocalStackDynamoSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

// 엔티티 클래스는 메인 코드에 있는 것을 그대로 사용한다.
import org.example.order.core.infra.dynamo.entity.OrderDynamoEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DynamoRepositoryIT extends LocalStackDynamoSupport {

    // ✅ 누락되었던 상수 정의
    private static final String TABLE = "order_it";

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhanced;
    private DynamoDbTable<OrderDynamoEntity> table;

    @BeforeEach
    void setUp() {
        // LocalStack 연결 클라이언트 생성
        this.dynamo = LocalStackDynamoSupport.dynamoDbClient();
        this.enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamo)
                .build();

        // 테이블 핸들 준비 (엔티티 스키마는 Bean 기반)
        this.table = enhanced.table(TABLE, TableSchema.fromBean(OrderDynamoEntity.class));

        // ✅ 누락되었던 ensureTable 로직: 존재하지 않으면 생성하고, 존재할 때 까지 대기
        ensureTable();
    }

    /**
     * 존재하지 않으면 Enhanced Client 로 테이블 생성 후, waiter 로 테이블 존재 상태까지 대기.
     * (키/인덱스 등은 엔티티 어노테이션/Schema 정의에 따름)
     */
    private void ensureTable() {
        boolean exists = true;
        try {
            dynamo.describeTable(DescribeTableRequest.builder().tableName(TABLE).build());
        } catch (ResourceNotFoundException e) {
            exists = false;
        }

        if (!exists) {
            // 엔티티 스키마 기반으로 테이블 생성
            table.createTable();
            // 생성 완료까지 waiter 로 대기
            try (DynamoDbWaiter waiter = dynamo.waiter()) {
                waiter.waitUntilTableExists(b -> b.tableName(TABLE)).matched();
            }
        }
    }

    @Test
    void context_and_table_ready() {
        // 간단한 가드 테스트: 테이블 핸들이 준비되어 있어야 한다.
        assertNotNull(table);
    }

    // 필요 시 실제 CRUD 케이스를 추가하면 됨.
    // @Test
    // void save_and_load() { ... }
}
