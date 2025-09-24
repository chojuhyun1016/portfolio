package org.example.order.core.infra.dynamo.migration.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.dynamo.migration.DynamoMigrationInitializer;
import org.example.order.core.infra.dynamo.migration.DynamoMigrationLoader;
import org.example.order.core.infra.dynamo.props.DynamoDbProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.ResourcePatternResolver;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * DynamoDB Migration AutoConfiguration
 * ------------------------------------------------------------------------
 * 활성 조건(AND):
 * - spring profile = local
 * - dynamodb.enabled = true
 * - dynamodb.auto-create = true
 * - DynamoDbClient 빈 존재
 * <p>
 * 기능:
 * - 애플리케이션 기동 시 "최신 버전(Vn)만" 병합 적용
 * - 테이블/인덱스(GSI/LSI) 생성 + 최신 시드 적용
 */
@Slf4j
@AutoConfiguration
@Profile("local")
@ConditionalOnClass(DynamoDbClient.class)
@ConditionalOnProperty(prefix = "dynamodb", name = {"enabled", "auto-create"}, havingValue = "true")
@EnableConfigurationProperties(DynamoDbProperties.class)
@RequiredArgsConstructor
public class DynamoMigrationAutoConfiguration {

    /**
     * Migration/Seed 로더
     * - classpath 경로에서 V*__*.json 스캔
     * - 최신 버전만 병합 적용
     * <p>
     * 주의:
     * - 로더의 생성자는 ResourcePatternResolver "1개"만 받는다.
     * - @Component 로 이미 등록되어 있으면 이 Bean은 생략됨(@ConditionalOnMissingBean).
     */
    @Bean
    @ConditionalOnMissingBean
    public DynamoMigrationLoader dynamoMigrationLoader(ResourcePatternResolver resolver) {
        return new DynamoMigrationLoader(resolver);
    }

    /**
     * 초기화(테이블/인덱스 생성 + 시드)
     */
    @Bean
    @ConditionalOnBean(DynamoDbClient.class)
    @ConditionalOnMissingBean
    public DynamoMigrationInitializer dynamoMigrationInitializer(
            DynamoDbClient client,
            DynamoDbProperties props,
            DynamoMigrationLoader loader
    ) {
        log.info("[DynamoMigration] AutoConfiguration enabled (local, auto-create=true)");

        return new DynamoMigrationInitializer(client, props, loader);
    }
}
