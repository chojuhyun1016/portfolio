package org.example.order.core.infra.dynamo.repository;

import org.example.order.core.infra.dynamo.repository.impl.OrderDynamoRepositoryImpl;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DynamoDB V2 기반 테스트 클래스
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ContextConfiguration(classes = {OrderDynamoRepositoryTest.DynamoDbTestConfig.class})
public class OrderDynamoRepositoryTest {

    private static final String TABLE_NAME = "order_dynamo";

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private OrderDynamoRepository orderDynamoRepository;
    private DynamoDbTable<OrderDynamoEntity> orderTable;

    /**
     * 테스트용 DynamoDB 설정 (로컬 스택 또는 DynamoDB Local)
     */
    @Configuration
    static class DynamoDbTestConfig {

        @Bean
        public DynamoDbClient dynamoDbClient() {
            return DynamoDbClient.builder()
                    .endpointOverride(URI.create("http://localhost:4566")) // LocalStack or DynamoDB Local
                    .region(software.amazon.awssdk.regions.Region.AP_NORTHEAST_2)
                    .build();
        }

        @Bean
        public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
            return DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();
        }
    }

    @BeforeAll
    void setUp() {
        this.orderDynamoRepository = new OrderDynamoRepositoryImpl(dynamoDbEnhancedClient);
        this.orderTable = dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(OrderDynamoEntity.class));

        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        ListTablesResponse tablesResponse = dynamoDbClient.listTables();

        if (!tablesResponse.tableNames().contains(TABLE_NAME)) {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            // ✅ GSI용 인덱스 생성 (테스트 환경용)
                            AttributeDefinition.builder()
                                    .attributeName("userId")
                                    .attributeType(ScalarAttributeType.N)
                                    .build()
                    )
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("userId-index")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("userId")
                                    .keyType(KeyType.HASH)
                                    .build())
                            .projection(Projection.builder()
                                    .projectionType(ProjectionType.ALL)
                                    .build())
                            .provisionedThroughput(ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build())
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);

            // 테이블 활성화까지 대기
            dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        }
    }

    @Test
    @Order(1)
    void save_and_findById_success() {
        String id = UUID.randomUUID().toString();
        OrderDynamoEntity entity = OrderDynamoEntity.builder()
                .id(id)
                .userId(1L)
                .orderNumber("ORD001")
                .orderPrice(10000L)
                .build();

        // when
        orderDynamoRepository.save(entity);
        Optional<OrderDynamoEntity> result = orderDynamoRepository.findById(id);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD001");
    }

    @Test
    @Order(2)
    void findByUserId_success() {
        Long userId = 999L;

        for (int i = 0; i < 3; i++) {
            orderDynamoRepository.save(OrderDynamoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .orderNumber("USER999_" + i)
                    .orderPrice(1000L + i)
                    .build());
        }

        // when
        List<OrderDynamoEntity> result = orderDynamoRepository.findByUserId(userId);

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.stream().allMatch(e -> e.getUserId().equals(userId))).isTrue();
    }

    @Test
    @Order(3)
    void findAll_success() {
        List<OrderDynamoEntity> result = orderDynamoRepository.findAll();
        assertThat(result).isNotEmpty();
    }
}
