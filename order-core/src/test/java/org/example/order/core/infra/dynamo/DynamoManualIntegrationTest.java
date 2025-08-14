package org.example.order.core.infra.dynamo;

import org.example.order.core.infra.dynamo.config.DynamoManualConfig;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.junit.jupiter.api.*;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual 모드 통합 테스트
 * <p>
 * - LocalStack(DynamoDB) 컨테이너를 띄워 실제 CRUD 검증
 * - ApplicationContextRunner 로 필요한 빈만 최소 기동
 * - 테이블은 테스트가 직접 생성/삭제 (모듈은 테이블 생성 책임 없음)
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DynamoManualIntegrationTest {

    // LocalStack (dynamodb 서비스만 활성화)
    @Container
    static final GenericContainer<?> localstack = new GenericContainer<>("localstack/localstack:1.4")
            .withEnv("SERVICES", "dynamodb")
            .withExposedPorts(4566);

    private ApplicationContextRunner ctx;
    private String endpoint;
    private final String tableName = "order_dynamo";

    @BeforeEach
    void initRunner() {
        endpoint = "http://" + localstack.getHost() + ":" + localstack.getMappedPort(4566);

        // Manual 모드: endpoint 또는 access/secret 중 하나라도 지정되면 Manual
        ctx = new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.endpoint=" + endpoint,
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.access-key=dummy",
                        "dynamodb.secret-key=dummy",
                        "dynamodb.table-name=" + tableName
                )
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class));
    }

    @Test
    @Order(1)
    void createTable_and_CRUD() {
        ctx.run(context -> {
            // 1) 클라이언트 꺼내서 테이블 생성 (없으면)
            DynamoDbClient client = context.getBean(DynamoDbClient.class);
            ensureTableExists(client, tableName);

            // 2) 리포지토리로 CRUD 검증
            OrderDynamoRepository repo = context.getBean(OrderDynamoRepository.class);

            // save
            var order = OrderDynamoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(1001L)
                    .orderNumber("ORD-12345")
                    .orderPrice(150_000L)
                    .build();

            repo.save(order);

            // findById
            Optional<OrderDynamoEntity> loaded = repo.findById(order.getId());
            assertThat(loaded).isPresent();
            assertThat(loaded.get().getOrderNumber()).isEqualTo("ORD-12345");

            // findAll (최소 1개 이상)
            List<OrderDynamoEntity> all = repo.findAll();
            assertThat(all).isNotEmpty();

            // findByUserId
            List<OrderDynamoEntity> byUser = repo.findByUserId(1001L);
            assertThat(byUser).extracting(OrderDynamoEntity::getUserId).contains(1001L);

            // deleteById
            repo.deleteById(order.getId());
            assertThat(repo.findById(order.getId())).isEmpty();
        });
    }

    @AfterEach
    void dropTable() {
        // 컨텍스트 없이 직접 SDK로 정리 (있어도 되고 없어도 됨)
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.AP_NORTHEAST_2)
                .build()) {
            ListTablesResponse tables = client.listTables();
            if (tables.tableNames().contains(tableName)) {
                client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 존재하지 않으면 테이블 생성
     */
    private static void ensureTableExists(DynamoDbClient client, String table) {
        ListTablesResponse list = client.listTables();
        if (!list.tableNames().contains(table)) {
            client.createTable(CreateTableRequest.builder()
                    .tableName(table)
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
                    )
                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            // ACTIVE 대기 (간단 폴링)
            waitActive(client, table);
        }
    }

    private static void waitActive(DynamoDbClient client, String table) {
        for (int i = 0; i < 30; i++) {
            DescribeTableResponse res = client.describeTable(DescribeTableRequest.builder().tableName(table).build());
            if (res.table().tableStatus() == TableStatus.ACTIVE) return;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new IllegalStateException("DynamoDB table not ACTIVE: " + table);
    }
}
