package org.example.order.core.infra.lock.support;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@DisabledIfEnvironmentVariable(named = "SKIP_TESTCONTAINERS", matches = "true")
public final class RedisTC {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    private RedisTC() {
    }

    public static void ensureStarted() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    public static void register(DynamicPropertyRegistry r) {
        String host = REDIS.getHost();
        Integer port = REDIS.getMappedPort(6379);
        String uri = "redis://" + host + ":" + port;

        r.add("lock.redisson.address", () -> uri);
        r.add("spring.data.redis.host", () -> host);
        r.add("spring.data.redis.port", () -> port);
        r.add("lock.redisson.database", () -> 0);
    }
}
