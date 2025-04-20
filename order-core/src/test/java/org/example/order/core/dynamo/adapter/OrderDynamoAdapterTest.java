//package org.example.order.core.dynamo.adapter;
//
//import com.amazonaws.auth.AWSStaticCredentialsProvider;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.client.builder.AwsClientBuilder;
//import com.amazonaws.services.dynamodbv2.*;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
//import com.amazonaws.services.dynamodbv2.model.*;
//import org.example.order.core.infra.dynamo.adapter.OrderDynamoAdapter;
//import org.example.order.core.infra.dynamo.config.DynamoDbProperties;
//import org.example.order.core.infra.dynamo.entity.OrderDynamoEntity;
//import org.example.order.core.infra.dynamo.port.out.OrderDynamoPort;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.*;
//import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
//
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringJUnitConfig(classes = OrderDynamoAdapterTest.TestDynamoConfig.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class OrderDynamoAdapterTest {
//
//    @TestConfiguration
//    static class TestDynamoConfig {
//
//        @Bean
//        public DynamoDbProperties dynamoDbProperties() {
//            DynamoDbProperties props = new DynamoDbProperties();
//            props.setEndpoint("http://localhost:4566");
//            props.setRegion("ap-northeast-2");
//            return props;
//        }
//
//        @Bean
//        public AmazonDynamoDB amazonDynamoDB(DynamoDbProperties properties) {
//            return AmazonDynamoDBClientBuilder.standard()
//                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
//                            properties.getEndpoint(), properties.getRegion()))
//                    .withCredentials(new AWSStaticCredentialsProvider(
//                            new BasicAWSCredentials("dummy-access-key", "dummy-secret-key")))
//                    .build();
//        }
//
//        @Bean
//        public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
//            return new DynamoDBMapper(amazonDynamoDB);
//        }
//
//        @Bean
//        public OrderDynamoPort orderDynamoPort(DynamoDBMapper dynamoDBMapper) {
//            return new OrderDynamoAdapter(dynamoDBMapper);
//        }
//    }
//
//    @Autowired private AmazonDynamoDB amazonDynamoDB;
//    @Autowired private OrderDynamoPort orderDynamoPort;
//
//    @BeforeEach
//    void setUp() {
//        if (!amazonDynamoDB.listTables().getTableNames().contains("order_dynamo")) {
//            amazonDynamoDB.createTable(new CreateTableRequest()
//                    .withTableName("order_dynamo")
//                    .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
//                    .withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.S))
//                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)));
//        }
//    }
//
//    @Test
//    void saveAndFindByIdTest() {
//        String id = UUID.randomUUID().toString();
//        OrderDynamoEntity entity = OrderDynamoEntity.builder()
//                .id(id)
//                .userId(100L)
//                .orderNumber("ORD-100")
//                .orderPrice(9999L)
//                .build();
//
//        orderDynamoPort.save(entity);
//        Optional<OrderDynamoEntity> result = orderDynamoPort.findById(id);
//
//        assertThat(result).isPresent();
//        assertThat(result.get().getOrderNumber()).isEqualTo("ORD-100");
//    }
//
//    @Test
//    void findByUserIdTest() {
//        Long userId = 200L;
//        for (int i = 0; i < 3; i++) {
//            orderDynamoPort.save(OrderDynamoEntity.builder()
//                    .id(UUID.randomUUID().toString())
//                    .userId(userId)
//                    .orderNumber("ORD-" + i)
//                    .orderPrice(1000L + i)
//                    .build());
//        }
//
//        List<OrderDynamoEntity> result = orderDynamoPort.findByUserId(userId);
//        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
//    }
//}
