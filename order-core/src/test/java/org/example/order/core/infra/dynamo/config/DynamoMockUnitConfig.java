package org.example.order.core.infra.dynamo.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * 단위 테스트용 DynamoDB Mock Config
 * <p>
 * - @Profile("test-unit") 에서만 활성화
 * - 실제 AWS 연결 없이 Mockito mock 반환
 */
@TestConfiguration
@Profile("test-unit")
public class DynamoMockUnitConfig {

    @Bean
    public DynamoDbClient dynamoDbClientMock() {
        return Mockito.mock(DynamoDbClient.class);
    }
}
