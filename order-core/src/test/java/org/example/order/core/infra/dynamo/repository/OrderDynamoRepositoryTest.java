package org.example.order.core.infra.dynamo.repository;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import org.example.order.core.infra.dynamo.config.DynamoDbProperties;
import org.example.order.core.infra.dynamo.model.OrderDynamoEntity;
import org.example.order.core.infra.dynamo.repository.impl.OrderDynamoRepositoryImpl;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderDynamoRepositoryTest {

    private OrderDynamoRepository orderDynamoRepository;
    private AmazonDynamoDB amazonDynamoDB;
    private DynamoDBMapper dynamoDBMapper;

    @BeforeAll
    void setUp() {
        // Dynamo 설정
        DynamoDbProperties props = new DynamoDbProperties();
        props.setEndpoint("http://localhost:4566"); // LocalStack endpoint
        props.setRegion("ap-northeast-2");

        amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        props.getEndpoint(), props.getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials("dummy-access-key", "dummy-secret-key")))
                .build();

        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
        orderDynamoRepository = new OrderDynamoRepositoryImpl(dynamoDBMapper);

        createTableIfNotExists("order_dynamo");
    }

    private void createTableIfNotExists(String tableName) {
        if (!amazonDynamoDB.listTables().getTableNames().contains(tableName)) {
            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)
                    .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
            amazonDynamoDB.createTable(request);
        }
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void save_and_findById_success() {
        // given
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
    @org.junit.jupiter.api.Order(2)
    void findByUserId_success() {
        // given
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
    @org.junit.jupiter.api.Order(3)
    void findAll_success() {
        List<OrderDynamoEntity> result = orderDynamoRepository.findAll();
        assertThat(result).isNotEmpty();
    }
}
