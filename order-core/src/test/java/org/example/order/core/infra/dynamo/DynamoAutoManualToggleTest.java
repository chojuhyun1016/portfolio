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
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.endpoint=http://localhost:4566",
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.table-name=order_dynamo"
                )
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class, DynamoAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).hasSingleBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_auto_condition_then_auto_loads_clients() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.region=ap-northeast-2"
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
