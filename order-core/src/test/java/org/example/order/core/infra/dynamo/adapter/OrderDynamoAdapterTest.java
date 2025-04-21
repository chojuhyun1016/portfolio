package org.example.order.core.dynamo.adapter;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import org.example.order.core.infra.dynamo.config.DynamoDbProperties;
import org.example.order.core.infra.dynamo.model.OrderDynamoEntity;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = OrderDynamoAdapterTest.TestDynamoConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderDynamoAdapterTest {

    @TestConfiguration
    static class TestDynamoConfig {

        @Bean
        public DynamoDbProperties dynamoDbProperties() {
            DynamoDbProperties props = new DynamoDbProperties();
            props.setEndpoint("http://localhost:4566"); // LocalStack endpoint
            props.setRegion("ap-northeast-2");
            return props;
        }

        @Bean
        public AmazonDynamoDB amazonDynamoDB(DynamoDbProperties properties) {
            return AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            properties.getEndpoint(), properties.getRegion()))
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials("dummy-access-key", "dummy-secret-key")))
                    .build();
        }

        @Bean
        public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
            return new DynamoDBMapper(amazonDynamoDB);
        }

        @Bean
        public OrderDynamoPort orderDynamoPort(DynamoDBMapper mapper) {
            return new OrderDynamoAdapter(mapper);
        }
    }

    @Autowired private AmazonDynamoDB amazonDynamoDB;
    @Autowired private OrderDynamoPort orderDynamoPort;

    private static final String TABLE_NAME = "order_dynamo";

    @BeforeAll
    void createTableIfNotExists() {
        if (!amazonDynamoDB.listTables().getTableNames().contains(TABLE_NAME)) {
            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(TABLE_NAME)
                    .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            amazonDynamoDB.createTable(request);
        }
    }

    @Test
    void saveAndFindById() {
        String id = UUID.randomUUID().toString();

        OrderDynamoEntity entity = OrderDynamoEntity.builder()
                .id(id)
                .userId(123L)
                .orderNumber("ORDER-123")
                .orderPrice(5000L)
                .build();

        orderDynamoPort.save(entity);

        var result = orderDynamoPort.findById(id);
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo("ORDER-123");
    }

    @Test
    void findByUserId() {
        Long userId = 999L;

        for (int i = 0; i < 3; i++) {
            orderDynamoPort.save(OrderDynamoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .orderNumber("ORDER-" + i)
                    .orderPrice(10000L + i)
                    .build());
        }

        var results = orderDynamoPort.findByUserId(userId);
        assertThat(results).hasSizeGreaterThanOrEqualTo(3);
        assertThat(results.stream().allMatch(r -> r.getUserId().equals(userId))).isTrue();
    }
}
