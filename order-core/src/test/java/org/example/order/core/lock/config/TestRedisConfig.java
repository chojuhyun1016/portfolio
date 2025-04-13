package org.example.order.core.lock.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestRedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(org.testcontainers.containers.GenericContainer<?> redisContainer) {
        String redisHost = redisContainer.getHost();
        Integer redisPort = redisContainer.getMappedPort(6379);

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);

        return Redisson.create(config);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public org.testcontainers.containers.GenericContainer<?> redisContainer() {
        return new org.testcontainers.containers.GenericContainer<>("redis:7.0.5")
                .withExposedPorts(6379);
    }
}
