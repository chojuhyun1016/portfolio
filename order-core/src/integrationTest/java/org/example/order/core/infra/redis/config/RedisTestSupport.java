package org.example.order.core.infra.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Redis 통합테스트 지원 (Testcontainers).
 *
 * - redis:7-alpine 컨테이너 기동 + Spring/Redisson 프로퍼티 동적 주입
 * - DataSource 와 무관
 */
@TestConfiguration
public class RedisTestSupport {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    static {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        final String host = REDIS.getHost();
        final Integer port = REDIS.getMappedPort(6379);
        final String uri = "redis://" + host + ":" + port;

        // Spring Data Redis
        r.add("spring.data.redis.host", () -> host);
        r.add("spring.data.redis.port", () -> port);

        // ✅ Redisson - 어떤 키를 읽더라도 안전
        r.add("lock.redisson.address", () -> uri); // 케이스 A
        r.add("lock.redisson.uri",     () -> uri); // 케이스 B
        r.add("lock.redisson.database", () -> 0);
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory cf,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        StringRedisSerializer key = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer val = new GenericJackson2JsonRedisSerializer(objectMapper.copy());

        template.setKeySerializer(key);
        template.setHashKeySerializer(key);
        template.setValueSerializer(val);
        template.setHashValueSerializer(val);
        template.afterPropertiesSet();
        return template;
    }
}
