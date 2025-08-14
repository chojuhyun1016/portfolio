package org.example.order.core.infra.dynamo;

import org.example.order.core.infra.dynamo.config.DynamoAutoConfig;
import org.example.order.core.infra.dynamo.config.DynamoManualConfig;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ON/OFF 및 Manual/Auto 조합에 따른 빈 로딩 결과 검증
 * - 네트워크 의존 없이 컨텍스트 레벨에서 빠르게 검사
 */
class DynamoAutoManualToggleTest {

    @Test
    void when_disabled_then_no_beans() {
        new ApplicationContextRunner()
                .withPropertyValues("dynamodb.enabled=false")
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class, DynamoAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(DynamoDbClient.class);
                    assertThat(ctx).doesNotHaveBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).doesNotHaveBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_manual_condition_then_manual_wins() {
        // endpoint 또는 access/secret 지정 → Manual 조건 충족
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.endpoint=http://localhost:4566",
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.table-name=order_dynamo"
                )
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class, DynamoAutoConfig.class))
                .run(ctx -> {
                    // 클라이언트/향상클라이언트 로드
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    // table-name 있으면 리포지토리 등록
                    assertThat(ctx).hasSingleBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_auto_condition_then_auto_loads_clients() {
        // Manual 조건 미충족: endpoint/access/secret 없음 → Auto
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.region=ap-northeast-2"
                        // table-name 미지정 → repo 미등록
                )
                .withConfiguration(UserConfigurations.of(DynamoAutoConfig.class, DynamoManualConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).doesNotHaveBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_auto_with_tableName_then_repo_registered() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.table-name=order_dynamo"
                )
                .withConfiguration(UserConfigurations.of(DynamoAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).hasSingleBean(OrderDynamoRepository.class);
                });
    }
}
