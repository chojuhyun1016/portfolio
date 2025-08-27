package org.example.order.core.infra.lock;

import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

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
@Testcontainers
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

        r.add("spring.data.redis.host", () -> host);
        r.add("spring.data.redis.port", () -> port);

        r.add("lock.redisson.address", () -> uri);

        r.add("lock.redisson.uri", () -> uri);

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
