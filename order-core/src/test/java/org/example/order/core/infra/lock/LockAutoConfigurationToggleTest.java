//package org.example.order.core.infra.lock;
//
//import org.example.order.core.infra.lock.aspect.DistributedLockAspect;
//import org.example.order.core.infra.lock.config.LockCoreTestSlice;
//import org.example.order.core.infra.lock.config.NamedLockMockEnabledConfig;
//import org.example.order.core.infra.lock.config.RedissonMockEnabledConfig;
//import org.example.order.core.infra.lock.factory.LockExecutorFactory;
//import org.example.order.core.infra.lock.factory.LockKeyGeneratorFactory;
//import org.example.order.core.infra.lock.key.LockKeyGenerator;
//import org.example.order.core.infra.lock.lock.LockExecutor;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.context.annotation.UserConfigurations;
//import org.springframework.boot.test.context.runner.ApplicationContextRunner;
//
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class LockAutoConfigurationToggleTest {
//
//    @Test
//    void when_lock_disabled_no_beans_loaded() {
//        new ApplicationContextRunner()
//                .withPropertyValues(
//                        "lock.enabled=false",
//                        "lock.named.enabled=false",
//                        "lock.redisson.enabled=false"
//                )
//                .withConfiguration(UserConfigurations.of(
//                        LockCoreTestSlice.class,
//                        NamedLockMockEnabledConfig.class,
//                        RedissonMockEnabledConfig.class
//                ))
//                .run(ctx -> {
//                    assertThat(ctx).doesNotHaveBean(LockKeyGeneratorFactory.class);
//                    assertThat(ctx).doesNotHaveBean(LockExecutorFactory.class);
//                    assertThat(ctx).doesNotHaveBean(DistributedLockAspect.class);
//                });
//    }
//
//    @Test
//    void when_lock_enabled_but_no_executors_generators_and_aspect_only() {
//        new ApplicationContextRunner()
//                .withPropertyValues(
//                        "lock.enabled=true",
//                        "lock.named.enabled=false",
//                        "lock.redisson.enabled=false"
//                )
//                .withConfiguration(UserConfigurations.of(LockCoreTestSlice.class))
//                .run(ctx -> {
//                    assertThat(ctx).hasSingleBean(LockKeyGeneratorFactory.class);
//                    assertThat(ctx).hasSingleBean(LockExecutorFactory.class);
//                    assertThat(ctx).hasSingleBean(DistributedLockAspect.class);
//
//                    Map<String, LockExecutor> executors = ctx.getBeansOfType(LockExecutor.class);
//                    assertThat(executors).isEmpty();
//
//                    assertThat(ctx.getBeansOfType(LockKeyGenerator.class)).isNotEmpty();
//                });
//    }
//
//    @Test
//    void when_named_enabled_named_executor_loaded() {
//        new ApplicationContextRunner()
//                .withPropertyValues(
//                        "lock.enabled=true",
//                        "lock.named.enabled=true",
//                        "lock.redisson.enabled=false"
//                )
//                .withConfiguration(UserConfigurations.of(
//                        LockCoreTestSlice.class,
//                        NamedLockMockEnabledConfig.class
//                ))
//                .run(ctx -> {
//                    Map<String, LockExecutor> executors = ctx.getBeansOfType(LockExecutor.class);
//                    assertThat(executors).containsKeys("namedLock");
//                });
//    }
//
//    @Test
//    void when_redisson_enabled_redisson_executor_loaded() {
//        new ApplicationContextRunner()
//                .withPropertyValues(
//                        "lock.enabled=true",
//                        "lock.named.enabled=false",
//                        "lock.redisson.enabled=true"
//                )
//                .withConfiguration(UserConfigurations.of(
//                        LockCoreTestSlice.class,
//                        RedissonMockEnabledConfig.class
//                ))
//                .run(ctx -> {
//                    Map<String, LockExecutor> executors = ctx.getBeansOfType(LockExecutor.class);
//                    assertThat(executors).containsKeys("redissonLock");
//                });
//    }
//}
