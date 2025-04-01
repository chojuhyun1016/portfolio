package org.example.order.core.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.redis.util.RedisSerializerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class RedisConfig {

    private static final int REDIS_COMMAND_TIMEOUT_SECONDS = 3;
    private static final int REDIS_SHUTDOWN_TIMEOUT_SECONDS = 3;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${redis.trusted-package}")
    private String redisTrustedPackage;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Initializing RedisConnectionFactory -> host: {}, port: {}, package: {}", redisHost, redisPort, redisTrustedPackage);

        var config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setPassword(redisPassword);

        var clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(REDIS_COMMAND_TIMEOUT_SECONDS))
                .shutdownTimeout(Duration.ofSeconds(REDIS_SHUTDOWN_TIMEOUT_SECONDS))
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        // RedisSerializerFactory 에 trusted package 주입
        var jsonSerializer = RedisSerializerFactory.create(redisTrustedPackage);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
