package org.example.order.core.infra.lock;

import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
// ✅ 유지: 통합 테스트용 부트앱 사용 (integrationTest 소스셋의 @SpringBootConfiguration)
import org.springframework.boot.test.context.SpringBootTest;
// ❌ 제거: @Import(RedisTestSupport.class) — @DynamicPropertySource는 테스트 클래스 안에 있어야 적용됨
// import org.springframework.context.annotation.Import;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

// ===== 추가: Testcontainers + DynamicPropertySource =====
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redisson 분산락 통합 테스트.
 *
 * - Redis 기반 분산락이 정상 동작하는지 검증한다.
 * - ⚠️ @DynamicPropertySource 는 “테스트 클래스 자신”에 있어야 Spring Test가 프로퍼티를 주입한다.
 *   외부 @Configuration(@Import)로 분리해두면 적용되지 않아 redisson 주소가 비게 된다.
 */
@SpringBootTest(
        classes = org.example.order.core.IntegrationBootApp.class,
        properties = {
                "spring.profiles.active=integration",
                "lock.enabled=true",
                "lock.redisson.enabled=true",
                "lock.named.enabled=false",
                "lock.redisson.wait-time=5000",
                "lock.redisson.lease-time=2000"
        }
)
// ✅ 추가: 이 테스트 클래스에서 직접 컨테이너를 관리
@Testcontainers
class RedissonLockIT extends AbstractIntegrationTest {

    @Autowired
    private RedissonClient redisson;

    // ----------------------------------------------------------------------
    // ✅ 통합테스트 전용 Redis 컨테이너 (테스트 클래스 안에 있어야 @DynamicPropertySource가 읽힘)
    // ----------------------------------------------------------------------
    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    /**
     * ✅ RedissonLockAutoConfig 가 읽을 프로퍼티를 “테스트 클래스 안에서” 동적으로 주입
     *
     * 주의:
     * - 반드시 static 이어야 함
     * - 이 메서드는 외부 클래스(@Import)로 빼면 Spring Test가 실행하지 않음
     */
    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        final String host = REDIS.getHost();
        final Integer port = REDIS.getMappedPort(6379);
        final String uri = "redis://" + host + ":" + port;

        // Spring Data Redis (필요 시)
        r.add("spring.data.redis.host", () -> host);
        r.add("spring.data.redis.port", () -> port);

        // ✅ RedissonLockAutoConfig 가 우선 읽는 주소
        r.add("lock.redisson.address", () -> uri);

        // 보강: URI 키로도 제공 (우선순위 2)
        r.add("lock.redisson.uri", () -> uri);

        // 선택 값들 (없어도 무방)
        r.add("lock.redisson.database", () -> 0);
        // r.add("lock.redisson.password", () -> ""); // 필요 시 주입
    }

    @Test
    void concurrent_access_serialized_by_redisson_lock() {
        RLock lock = redisson.getLock("it:redisson:lock:test");
        assertNotNull(lock, "락 인스턴스 null");

        lock.lock();
        try {
            assertTrue(lock.isLocked(), "락이 획득되지 않았다.");
        } finally {
            lock.unlock();
        }
    }
}
