package org.example.order.core.infra.lock;

import org.example.order.core.infra.lock.aspect.DistributedLockAspect;
import org.example.order.core.infra.lock.config.LockManualConfig;
import org.example.order.core.infra.lock.config.NamedLockAutoConfig;
import org.example.order.core.infra.lock.config.NamedLockProperties;
import org.example.order.core.infra.lock.config.RedissonLockAutoConfig;
import org.example.order.core.infra.lock.config.RedissonLockProperties;
import org.example.order.core.infra.lock.factory.LockExecutorFactory;
import org.example.order.core.infra.lock.factory.LockKeyGeneratorFactory;
import org.example.order.core.infra.lock.key.LockKeyGenerator;
import org.example.order.core.infra.lock.lock.LockExecutor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 설정 토글 가벼운 검증 (빈 로딩 결과만 확인)
 * - lock.enabled=false: 어떤 빈도 생성되지 않음
 * - lock.enabled=true & executors OFF: KeyGenerator/Factory/Aspect만 생성, Executor는 없음
 * - lock.enabled=true & named.enabled=true: NamedLock Executor 생성 (DataSource 주입 가정)
 * - lock.enabled=true & redisson.enabled=true: RedissonLock Executor 생성 (RedissonClient 주입 가정)
 */
class LockAutoConfigurationToggleTest {

    @Test
    void when_lock_disabled_no_beans_loaded() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "lock.enabled=false",
                        "lock.named.enabled=false",
                        "lock.redisson.enabled=false"
                )
                .withConfiguration(UserConfigurations.of(LockManualConfig.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(LockKeyGeneratorFactory.class);
                    assertThat(ctx).doesNotHaveBean(LockExecutorFactory.class);
                    assertThat(ctx).doesNotHaveBean(DistributedLockAspect.class);
                });
    }

    @Test
    void when_lock_enabled_but_no_executors_generators_and_aspect_only() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "lock.enabled=true",
                        "lock.named.enabled=false",
                        "lock.redisson.enabled=false"
                )
                .withConfiguration(UserConfigurations.of(LockManualConfig.class))
                .run(ctx -> {
                    // KeyGenerators, Factories, Aspect는 등록
                    assertThat(ctx).hasSingleBean(LockKeyGeneratorFactory.class);
                    assertThat(ctx).hasSingleBean(LockExecutorFactory.class);
                    assertThat(ctx).hasSingleBean(DistributedLockAspect.class);

                    // Executors는 없음
                    Map<String, LockExecutor> executors = ctx.getBeansOfType(LockExecutor.class);
                    assertThat(executors).isEmpty();

                    // 기본 KeyGenerator 등록 확인(sha256/simple/spell)
                    assertThat(ctx.getBeansOfType(LockKeyGenerator.class)).isNotEmpty();
                });
    }

    @Test
    void when_named_enabled_named_executor_loaded() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "lock.enabled=true",
                        "lock.named.enabled=true",
                        "lock.redisson.enabled=false",
                        // NamedLockProperties default를 쓸 것이므로 별도 설정 불필요
                        "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL",
                        "spring.datasource.driverClassName=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password="
                )
                .withConfiguration(UserConfigurations.of(
                        LockManualConfig.class,
                        NamedLockAutoConfig.class // NamedLockProperties 바인딩
                ))
                // DataSource 제공(간단히 H2 사용) → 실제 실행은 안 하므로 함수 유무는 상관 X
                .withBean(DataSource.class, () -> {
                    DriverManagerDataSource ds = new DriverManagerDataSource();
                    ds.setUrl("jdbc:h2:mem:test;MODE=MySQL");
                    ds.setDriverClassName("org.h2.Driver");
                    ds.setUsername("sa");
                    ds.setPassword("");
                    return ds;
                })
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(NamedLockProperties.class);
                    Map<String, LockExecutor> executors = ctx.getBeansOfType(LockExecutor.class);
                    assertThat(executors).containsKeys("namedLock");
                });
    }

    @Test
    void when_redisson_enabled_redisson_executor_loaded() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "lock.enabled=true",
                        "lock.named.enabled=false",
                        "lock.redisson.enabled=true"
                )
                .withConfiguration(UserConfigurations.of(
                        LockManualConfig.class,
                        RedissonLockAutoConfig.class // RedissonClient 자동 구성과 별개로, 여기선 직접 주입
                ))
                // RedissonClient는 모킹으로 대체(실행 안 함)
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(RedissonLockProperties.class, () -> {
                    RedissonLockProperties props = new RedissonLockProperties();
                    props.setAddress("redis://127.0.0.1:6379");
                    props.setDatabase(0);
                    return props;
                })
                .run(ctx -> {
                    Map<String, LockExecutor> executors = ctx.getBeansOfType(LockExecutor.class);
                    assertThat(executors).containsKeys("redissonLock");
                });
    }
}
