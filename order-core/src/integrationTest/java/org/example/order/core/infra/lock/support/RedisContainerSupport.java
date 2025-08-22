package org.example.order.core.infra.lock.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Redis(Testcontainers) 통합 테스트 공통 베이스
 * - redissonLock 사용을 위해 lock.redisson.* 속성 동적 주입
 * - 충돌 방지를 위해 Redisson Starter 및 Spring Data Redis 오토설정 제외
 * <p>
 * 주의:
 * - 프로덕션 코드는 변경하지 않는다.
 */
@Testcontainers
public abstract class RedisContainerSupport {

    private static final int REDIS_PORT = 6379;

    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void registerRedisProps(DynamicPropertyRegistry registry) {
        registry.add("lock.enabled", () -> "true");
        registry.add("lock.redisson.enabled", () -> "true");
        registry.add("lock.named.enabled", () -> "false");

        registry.add("lock.redisson.address",
                () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(REDIS_PORT));
        registry.add("lock.redisson.database", () -> "0");

        // 테스트 격리를 위한 오토설정 제외(기존 문제 회피)
        registry.add("spring.autoconfigure.exclude",
                () -> String.join(",",
                        "org.redisson.spring.starter.RedissonAutoConfiguration",
                        "org.redisson.spring.starter.RedissonAutoConfigurationV2",
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
                ));
    }

    @BeforeAll
    static void beforeAll() {
        REDIS.start();
    }

    @AfterAll
    static void afterAll() {
        REDIS.stop();
    }
}
