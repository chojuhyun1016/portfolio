package org.example.order.core.redis.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.redis.util.RedisSerializerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@RequiredArgsConstructor
public class RedisConfig {

    private static final int REDIS_COMMAND_TIMEOUT_SECONDS = 3;
    private static final int REDIS_SHUTDOWN_TIMEOUT_SECONDS = 3;

    private final RedisProperties redisProperties;
    private LettuceConnectionFactory lettuceConnectionFactory;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Initializing RedisConnectionFactory -> host: {}, port: {}, trustedPackage: {}",
                redisProperties.getHost(), redisProperties.getPort(), redisProperties.getTrustedPackage());

        var config = new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
        config.setPassword(redisProperties.getPassword());

        var clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(REDIS_COMMAND_TIMEOUT_SECONDS))
                .shutdownTimeout(Duration.ofSeconds(REDIS_SHUTDOWN_TIMEOUT_SECONDS))
                .build();

        this.lettuceConnectionFactory = new LettuceConnectionFactory(config, clientConfig);
        return this.lettuceConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        var trustedPackage = redisProperties.getTrustedPackage();

        if (trustedPackage == null || trustedPackage.trim().isEmpty()) {
            trustedPackage = "org.example";
            log.warn("Redis trustedPackage is not configured. Using default: {}", trustedPackage);
        }

        var jsonSerializer = RedisSerializerFactory.create(trustedPackage);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @PreDestroy
    public void destroy() {
        if (lettuceConnectionFactory != null) {
            log.info("Shutting down Redis connection factory...");
            lettuceConnectionFactory.destroy();
        }
    }
}
