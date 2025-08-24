package org.example.order.core.infra.lock.support;

/**
 * 통합테스트 전용 Redis Testcontainers 지원 유틸.
 *
 * - @Container 정적 Redis 컨테이너 사용
 * - @DynamicPropertySource 로 Redisson 설정 프로퍼티를 등록
 *   (람다로 등록해야 컨테이너가 실제로 뜬 뒤 getMappedPort를 호출함)
 */

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class RedisContainerSupport {

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7.2.4");

    @Container
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(REDIS_IMAGE)
                    .withExposedPorts(6379);

    /**
     * RedissonLockAutoConfig 가 바인딩할 프로퍼티를 동적으로 제공.
     * 이때 반드시 람다로 등록해야 컨테이너가 시작된 뒤 포트를 읽는다.
     */
    @DynamicPropertySource
    static void registerRedisProps(DynamicPropertyRegistry registry) {
        registry.add("lock.redisson.address", () ->
                String.format("redis://%s:%d", REDIS.getHost(), REDIS.getMappedPort(6379)));
        // 필요 시 비밀번호/DB index 등 추가
        // registry.add("lock.redisson.password", () -> "");
        // registry.add("lock.redisson.database", () -> 0);
    }
}
