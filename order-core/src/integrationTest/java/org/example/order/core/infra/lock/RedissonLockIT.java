package org.example.order.core.infra.lock;

import org.example.order.core.IntegrationBoot;
import org.example.order.core.infra.lock.config.LockInfraConfig; // ★ 단일 구성 사용
import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedissonLockIT
 * <p>
 * 기존 주석/구조 유지.
 * - application-integration.yml 은 기본 OFF (named/redisson 모두 false)
 * - 테스트에서만 redisson=ON, named=OFF 를 명시 + Redis endpoint 주입
 * - Starter 자동구성은 제외(이 테스트는 LockInfraConfig 가 만드는 RedissonClient 를 사용)
 */
@SpringBootTest(
        classes = IntegrationBoot.class,
        properties = {
                "spring.profiles.active=integration",
                "lock.enabled=true",
                "lock.named.enabled=false",
                "lock.redisson.enabled=true",
                "lock.redisson.wait-time=5000",
                "lock.redisson.lease-time=2000"
        }
)
@Testcontainers
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@Import(LockInfraConfig.class) // ★ 새 구성 조립
class RedissonLockIT extends AbstractIntegrationTest {

    @Autowired
    private RedissonClient redisson;

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        final String host = REDIS.getHost();
        final Integer port = REDIS.getMappedPort(6379);
        final String uri = "redis://" + host + ":" + port;

        // LockInfraConfig → RedissonClient 구성에 사용
        r.add("lock.redisson.address", () -> uri);

        // (선택) spring.data.redis.* 를 읽는 환경이 있다면 함께 주입 가능
        r.add("spring.data.redis.host", () -> host);
        r.add("spring.data.redis.port", () -> port);

        r.add("lock.redisson.database", () -> 0);
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
