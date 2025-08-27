package org.example.order.core.infra.dynamo.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@TestConfiguration
@Profile("test-unit")
public class DynamoMockUnitConfig {

    @Bean
    public DynamoDbClient dynamoDbClientMock() {
        return Mockito.mock(DynamoDbClient.class);
    }
}
