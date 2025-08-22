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

/**
 * Redis ON/OFF 및 Manual/Auto 조합에 따른 빈 로딩 결과 검증
 * - 네트워크 의존 없이 컨텍스트 레벨에서 빠르게 검사
 * - Dynamo 스타일과 동일한 형태
 */
class RedisAutoManualToggleTest {

    @Test
    void when_disabled_then_no_beans() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.redis.enabled=false",
                        "spring.profiles.active=local" // @Profile('!test') 통과를 위해 test가 아닌 프로필
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
        // Manual 조건: enabled=true AND uri 존재 AND (host,port 미지정)
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
        // Auto 조건: enabled=true AND host/port 기반
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
        // Manual은 "uri 있고 host/port 비어있을 때"만 활성화 → host 지정되면 Auto
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
