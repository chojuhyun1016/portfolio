package org.example.order.core.infra.dynamo.config;

import org.example.order.core.infra.dynamo.props.DynamoDbProperties;
import org.example.order.core.infra.dynamo.repository.impl.OrderDynamoRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * DynamoDB Auto 구성 (수동 조건이 아닐 때 동작)
 * <p>
 * 활성 조건:
 * - dynamodb.enabled=true
 * - 그리고 ManualConfig 의 수동 조건이 "아닌" 경우
 * (즉, endpoint 미지정 && access/secret 미지정)
 * <p>
 * 특징:
 * - DefaultCredentialsProvider 로 자격증명 자동 탐지(IAM/ENV/Shared)
 * - region 은 설정되어 있으면 사용, 없으면 SDK 기본 탐지
 * - table-name 이 있을 때만 리포지토리 등록
 */
@Configuration
@EnableConfigurationProperties(DynamoDbProperties.class)
@ConditionalOnProperty(prefix = "dynamodb", name = "enabled", havingValue = "true")
public class DynamoAutoConfig {

    /**
     * Manual 경로에서 DynamoDbClient 를 만들지 않았다면(=MissingBean),
     * Auto 경로로 기본 탐지 기반의 클라이언트를 만든다.
     */
    @Bean
    @ConditionalOnMissingBean(DynamoDbClient.class)
    public DynamoDbClient dynamoDbClient(DynamoDbProperties props) {
        var builder = DynamoDbClient.builder();

        // region: 지정되어 있으면 사용, 아니면 SDK 기본 탐지
        if (props.getRegion() != null && !props.getRegion().isBlank()) {
            builder.region(Region.of(props.getRegion()));
        }

        // 자격증명: 기본 공급자
        builder.credentialsProvider(DefaultCredentialsProvider.create());

        return builder.build();
    }

    /**
     * Auto 경로로 등록된 Client 가 있을 때 EnhancedClient 등록
     */
    @Bean
    @ConditionalOnBean(DynamoDbClient.class)
    @ConditionalOnMissingBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    /**
     * table-name 이 존재할 때만 리포지토리 등록
     */
    @Bean
    @ConditionalOnBean(DynamoDbEnhancedClient.class)
    @ConditionalOnProperty(prefix = "dynamodb", name = "table-name")
    @ConditionalOnMissingBean
    public OrderDynamoRepositoryImpl orderDynamoRepository(DynamoDbEnhancedClient enhancedClient,
                                                           DynamoDbProperties props) {
        return new OrderDynamoRepositoryImpl(enhancedClient, props.getTableName());
    }
}
