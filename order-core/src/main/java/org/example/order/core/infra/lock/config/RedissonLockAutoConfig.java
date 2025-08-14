package org.example.order.core.infra.lock.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedissonLockProperties.class)
@ConditionalOnClass(Redisson.class)
@ConditionalOnProperty(name = {"lock.enabled", "lock.redisson.enabled"}, havingValue = "true")
public class RedissonLockAutoConfig {

    private final RedissonLockProperties redissonLockProperties;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(redissonLockProperties.getAddress())
                .setDatabase(redissonLockProperties.getDatabase());

        if (redissonLockProperties.getPassword() != null && !redissonLockProperties.getPassword().isBlank()) {
            config.useSingleServer().setPassword(redissonLockProperties.getPassword());
        }

        return Redisson.create(config);
    }
}
