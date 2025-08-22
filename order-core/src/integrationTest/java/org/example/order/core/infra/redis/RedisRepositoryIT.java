package org.example.order.core.infra.redis;

import org.example.order.core.infra.redis.repository.RedisRepository;
import org.example.order.core.infra.redis.repository.impl.RedisRepositoryImpl;
import org.example.order.core.infra.redis.support.RedisSerializerFactory;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers 기반 Redis 통합 테스트
 * - 도커 컨테이너(redis:7-alpine)를 띄워 실연결 검증
 * - 원본 코드/주석 손대지 않음 (@Profile("!test")로 인해 테스트 전용 구성 사용)
 */
@Testcontainers
@SpringBootTest(classes = RedisRepositoryIT.TestRedisConfig.class)
class RedisRepositoryIT {

    private static final String PREFIX = "it:redis:";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @TestConfiguration
    static class TestRedisConfig {

        @Bean
        LettuceConnectionFactory redisConnectionFactory() {
            String host = redis.getHost();
            Integer port = redis.getMappedPort(6379);

            RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
            standalone.setPassword(RedisPassword.none());

            LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(5))
                    .shutdownTimeout(Duration.ofSeconds(3))
                    .build();

            return new LettuceConnectionFactory(standalone, client);
        }

        @Bean
        RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory cf) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(cf);

            StringRedisSerializer key = new StringRedisSerializer();
            var json = RedisSerializerFactory.create("org.example.order");

            template.setKeySerializer(key);
            template.setValueSerializer(json);
            template.setHashKeySerializer(key);
            template.setHashValueSerializer(json);

            template.setEnableTransactionSupport(true); // tx 테스트
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        RedisRepository redisRepository(RedisTemplate<String, Object> template) {
            return new RedisRepositoryImpl(template);
        }
    }

    @BeforeEach
    void cleanAllKeys(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") RedisTemplate<String, Object> template) {
        Set<String> keys = template.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            template.delete(keys);
        }
    }

    @Test
    void end_to_end_repository_functions(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") RedisRepository repo) {
        // 값/해시/리스트/셋/정렬셋/TTL/키/트랜잭션 전체 동작 확인
        String v = PREFIX + "v";
        repo.set(v, Map.of("a", 1));
        assertThat(repo.get(v)).isInstanceOf(Map.class);

        String h = PREFIX + "h";
        repo.putAllHash(h, Map.of("x", 10, "y", 20));
        assertThat(repo.getHash(h, "x")).isEqualTo(10);

        String l = PREFIX + "l";
        repo.leftPush(l, "a");
        repo.leftPush(l, "b");
        assertThat(repo.listSize(l)).isGreaterThanOrEqualTo(2);

        String s = PREFIX + "s";
        repo.addAllSet(s, List.of("p", "q", "r"));
        assertThat(repo.getSetMembers(s)).contains("p", "q", "r");

        String z = PREFIX + "z";
        repo.zAdd(z, "u1", 1.0);
        repo.zAdd(z, "u2", 2.0);
        assertThat(repo.zRangeByScore(z, 1.0, 2.0)).contains("u1", "u2");

        String t = PREFIX + "t";
        repo.set(t, "ttl");
        assertThat(repo.expire(t, 5)).isTrue();
        assertThat(repo.getExpire(t)).isGreaterThan(0);

        String th = PREFIX + "th";
        repo.transactionPutAllHash(th, Map.of("a", 1, "b", 2));
        assertThat(repo.getHash(th, "b")).isEqualTo(2);
    }
}
