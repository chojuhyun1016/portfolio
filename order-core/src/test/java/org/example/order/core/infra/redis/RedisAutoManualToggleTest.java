package org.example.order.core.infra.redis;

import org.example.order.core.infra.redis.config.RedisAutoConfig;
import org.example.order.core.infra.redis.config.RedisCommonConfig;
import org.example.order.core.infra.redis.config.RedisManualConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RedisAutoManualToggleTest {

    @Test
    void when_disabled_then_no_beans() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.redis.enabled=false",
                        "spring.profiles.active=local"
                )
                .withConfiguration(UserConfigurations.of(
                        RedisManualConfig.class, RedisAutoConfig.class, RedisCommonConfig.class
                ))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RedisConnectionFactory.class);
                    assertThat(ctx).doesNotHaveBean(RedisTemplate.class);
                });
    }

    @Test
    void when_manual_condition_with_uri_only_then_manual_loads() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.redis.enabled=true",
                        "spring.redis.uri=redis://127.0.0.1:6379",
                        "spring.profiles.active=local"
                )
                .withConfiguration(UserConfigurations.of(
                        RedisManualConfig.class, RedisAutoConfig.class, RedisCommonConfig.class
                ))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RedisConnectionFactory.class);
                    assertThat(ctx).hasSingleBean(RedisTemplate.class);
                });
    }

    @Test
    void when_auto_condition_with_host_port_then_auto_loads() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.redis.enabled=true",
                        "spring.redis.host=127.0.0.1",
                        "spring.redis.port=6379",
                        "spring.profiles.active=local"
                )
                .withConfiguration(UserConfigurations.of(
                        RedisAutoConfig.class, RedisManualConfig.class, RedisCommonConfig.class
                ))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RedisConnectionFactory.class);
                    assertThat(ctx).hasSingleBean(RedisTemplate.class);
                });
    }

    @Test
    void when_uri_and_host_together_then_auto_selected() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.redis.enabled=true",
                        "spring.redis.uri=redis://127.0.0.1:6379",
                        "spring.redis.host=127.0.0.1",
                        "spring.profiles.active=local"
                )
                .withConfiguration(UserConfigurations.of(
                        RedisAutoConfig.class, RedisManualConfig.class, RedisCommonConfig.class
                ))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RedisConnectionFactory.class);
                    assertThat(ctx).hasSingleBean(RedisTemplate.class);
                });
    }
}
