package org.example.order.core.infra.lock.support;

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

    @DynamicPropertySource
    static void registerRedisProps(DynamicPropertyRegistry registry) {
        registry.add("lock.redisson.address", () ->
                String.format("redis://%s:%d", REDIS.getHost(), REDIS.getMappedPort(6379)));
    }
}
