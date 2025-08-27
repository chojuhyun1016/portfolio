package org.example.order.core.infra.dynamo;

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

import org.example.order.core.infra.dynamo.entity.OrderDynamoEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DynamoRepositoryIT extends LocalStackDynamoSupport {

    private static final String TABLE = "order_it";

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhanced;
    private DynamoDbTable<OrderDynamoEntity> table;

    @BeforeEach
    void setUp() {
        this.dynamo = LocalStackDynamoSupport.dynamoDbClient();
        this.enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamo)
                .build();

        this.table = enhanced.table(TABLE, TableSchema.fromBean(OrderDynamoEntity.class));

        ensureTable();
    }

    private void ensureTable() {
        boolean exists = true;

        try {
            dynamo.describeTable(DescribeTableRequest.builder().tableName(TABLE).build());
        } catch (ResourceNotFoundException e) {
            exists = false;
        }

        if (!exists) {
            table.createTable();

            try (DynamoDbWaiter waiter = dynamo.waiter()) {
                waiter.waitUntilTableExists(b -> b.tableName(TABLE)).matched();
            }
        }
    }

    @Test
    void context_and_table_ready() {
        assertNotNull(table);
    }
}
