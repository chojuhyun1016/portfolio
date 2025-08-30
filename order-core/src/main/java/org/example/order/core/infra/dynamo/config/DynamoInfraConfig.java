package org.example.order.core.infra.dynamo.config;

import lombok.extern.slf4j.Slf4j;
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
 * DynamoDB 단일 구성 (설정 기반 + @Import 조립)
 * <p>
 * 활성 조건:
 * - dynamodb.enabled=true 일 때만 모듈 동작
 * <p>
 * 특징:
 * - 하나의 구성에서 수동/자동 경로를 모두 처리:
 * - endpoint 지정 시: endpoint + (region 없으면 로컬 개발 호환을 위해 us-east-1 기본값) + Static/Default Credentials
 * - access-key/secret-key 지정 시: StaticCredentialsProvider 사용
 * - 그 외: DefaultCredentialsProvider / Region은 설정 있으면 반영, 없으면 SDK 기본 탐지
 * - table-name 이 있을 때에만 OrderDynamoRepository 빈 등록
 * <p>
 * 사용법:
 *
 * @Import(DynamoInfraConfig.class) + application.yml 의 dynamodb.* 프로퍼티로 제어
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DynamoDbProperties.class)
@ConditionalOnProperty(prefix = "dynamodb", name = "enabled", havingValue = "true")
public class DynamoInfraConfig {

    /* ---------------- DynamoDbClient ---------------- */
    @Bean
    @ConditionalOnMissingBean(DynamoDbClient.class)
    public DynamoDbClient dynamoDbClient(DynamoDbProperties props) {
        var builder = DynamoDbClient.builder();

        boolean hasEndpoint = hasText(props.getEndpoint());
        boolean hasAccess = hasText(props.getAccessKey());
        boolean hasSecret = hasText(props.getSecretKey());
        boolean hasRegion = hasText(props.getRegion());

        // (1) 엔드포인트가 명시된 경우(LocalStack 등)
        if (hasEndpoint) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
            // Region 미지정 시 SDK가 요구하므로 로컬 개발 기본값 제공
            Region region = hasRegion ? Region.of(props.getRegion()) : Region.US_EAST_1;
            builder.region(region);

            if (hasAccess && hasSecret) {
                builder.credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                        )
                );
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            log.info("[Dynamo] manual(endpoint) mode: endpoint={}, region={}", props.getEndpoint(), region.id());

            return builder.build();
        }

        // (2) 엔드포인트는 없지만 access/secret 지정된 경우
        if (hasAccess && hasSecret) {
            if (hasRegion) builder.region(Region.of(props.getRegion()));
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                    )
            );

            log.info("[Dynamo] manual(credentials) mode: region={}", hasRegion ? props.getRegion() : "(provider default)");

            return builder.build();
        }

        // (3) 자동 탐지(DefaultCredentialsProvider + Region 설정 있으면 반영)
        if (hasRegion) {
            builder.region(Region.of(props.getRegion()));
        }

        builder.credentialsProvider(DefaultCredentialsProvider.create());

        log.info("[Dynamo] auto(discovery) mode: region={}", hasRegion ? props.getRegion() : "(provider default)");

        return builder.build();
    }

    /* ---------------- Enhanced Client ---------------- */
    @Bean
    @ConditionalOnBean(DynamoDbClient.class)
    @ConditionalOnMissingBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    /* ---------------- Repository (옵션) ---------------- */
    @Bean
    @ConditionalOnBean(DynamoDbEnhancedClient.class)
    @ConditionalOnProperty(prefix = "dynamodb", name = "table-name")
    @ConditionalOnMissingBean
    public OrderDynamoRepositoryImpl orderDynamoRepository(DynamoDbEnhancedClient enhancedClient,
                                                           DynamoDbProperties props) {
        return new OrderDynamoRepositoryImpl(enhancedClient, props.getTableName());
    }

    /* ---------------- Utils ---------------- */
    private static boolean hasText(String v) {
        return v != null && !v.isBlank();
    }
}
