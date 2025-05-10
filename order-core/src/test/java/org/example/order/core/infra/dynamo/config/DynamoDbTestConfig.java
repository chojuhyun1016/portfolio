package org.example.order.core.infra.dynamo.config;

import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.List;

/**
 * DynamoDB 테스트 환경 설정
 *
 * - LocalStack를 대상으로 DynamoDB 클라이언트 + 테이블 초기화
 */
@Getter
public class DynamoDbTestConfig {

    private final String tableName = "order_dynamo";
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTable<?> table;

    public DynamoDbTestConfig() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy-access-key", "dummy-secret-key")
                ))
                .build();

        createTableIfNotExists();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        this.table = enhancedClient.table(tableName, TableSchema.fromBean(org.example.order.domain.order.entity.OrderDynamoEntity.class));
    }

    private void createTableIfNotExists() {
        ListTablesResponse tables = dynamoDbClient.listTables();
        if (!tables.tableNames().contains(tableName)) {
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }

    public void deleteTableIfExists() {
        ListTablesResponse tables = dynamoDbClient.listTables();
        if (tables.tableNames().contains(tableName)) {
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
        }
    }

    @SuppressWarnings("unchecked")
    public DynamoDbTable<org.example.order.domain.order.entity.OrderDynamoEntity> getOrderTable() {
        return (DynamoDbTable<org.example.order.domain.order.entity.OrderDynamoEntity>) table;
    }
}
