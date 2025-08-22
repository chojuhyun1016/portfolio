package org.example.order.core.infra.dynamo.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * 유닛 테스트용 DynamoDB Mock
 * - 실 서버/에뮬레이터 미사용 (가벼움)
 * - @ActiveProfiles("test-unit") 에서만 활성화
 */
@TestConfiguration
@Profile("test-unit")
public class DynamoMockUnitConfig {

    @Bean
    public DynamoDbClient dynamoDbClientMock() {
        return Mockito.mock(DynamoDbClient.class);
    }
}
