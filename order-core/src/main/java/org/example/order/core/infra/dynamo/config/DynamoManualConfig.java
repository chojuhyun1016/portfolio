package org.example.order.core.infra.dynamo.config;

import org.example.order.core.infra.dynamo.props.DynamoDbProperties;
import org.example.order.core.infra.dynamo.repository.impl.OrderDynamoRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * DynamoDB Manual 구성
 * <p>
 * 활성 조건:
 * - dynamodb.enabled=true
 * - 그리고 아래 "수동 조건" 중 하나라도 true:
 * a) dynamodb.endpoint 가 설정되어 있음
 * b) dynamodb.access-key 과 dynamodb.secret-key 가 모두 설정되어 있음
 * <p>
 * 특징:
 * - endpoint/region/accessKey/secretKey 를 그대로 반영하여 DynamoDbClient 생성
 * - EnhancedClient 등록
 * - table-name 이 있을 때에만 OrderDynamoRepository 등록 (기본/시딩 없음)
 */
@Configuration
@EnableConfigurationProperties(DynamoDbProperties.class)
@ConditionalOnProperty(prefix = "dynamodb", name = "enabled", havingValue = "true")
public class DynamoManualConfig {

    /**
     * "수동 조건" 판별:
     * - endpoint 가 있거나
     * - access-key 와 secret-key 가 둘 다 있으면
     * Manual 클라이언트를 생성한다.
     * <p>
     * SpEL을 사용한 조건식(OR)로 처리.
     */
    @Bean
    @ConditionalOnExpression(
            "#{ '${dynamodb.endpoint:}'.length() > 0 or " +
                    "   ( '${dynamodb.access-key:}'.length() > 0 and '${dynamodb.secret-key:}'.length() > 0 ) }"
    )
    @ConditionalOnMissingBean
    public DynamoDbClient dynamoDbClient(DynamoDbProperties props) {
        var builder = DynamoDbClient.builder();

        // endpoint (LocalStack 등)
        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
        }

        // region (권장)
        if (props.getRegion() != null && !props.getRegion().isBlank()) {
            builder.region(Region.of(props.getRegion()));
        }

        // credentials: access+secret 모두 있으면 Static, 없으면 Default
        if (props.getAccessKey() != null && !props.getAccessKey().isBlank()
                && props.getSecretKey() != null && !props.getSecretKey().isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                    )
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    /**
     * Manual 경로로 생성된 DynamoDbClient 가 있을 때 EnhancedClient 등록
     */
    @Bean
    @ConditionalOnBean(DynamoDbClient.class)
    @ConditionalOnMissingBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    /**
     * 리포지토리 등록:
     * - table-name 이 명시되어 있을 때만
     * - EnhancedClient 가 이미 존재할 때만
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
