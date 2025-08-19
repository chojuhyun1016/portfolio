package org.example.order.core.infra.redis;

import org.example.order.core.infra.redis.config.RedisAutoConfig;
import org.example.order.core.infra.redis.config.RedisCommonConfig;
import org.example.order.core.infra.redis.props.RedisProperties;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.example.order.core.infra.redis.repository.impl.RedisRepositoryImpl;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Auto 모드(C-방식 대체):
 * - host/port를 Testcontainers에서 동적으로 받아 spring.redis.*에 주입
 * - @ServiceConnection 사용하지 않음(IDE/해결 문제 배제)
 * - URI는 제공하지 않음(= 자동 설정 경로)
 */
@SpringBootTest(
        classes = {
                RedisAutoConfig.class,
                RedisCommonConfig.class,
                RedisAutoRepositoryIT.TestBeans.class
        },
        properties = {
                "spring.redis.enabled=true",
                "spring.redis.trusted-package=org.example.order",
                "spring.redis.client-name=order-core-it-auto"
        }
)
@Testcontainers
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(RedisAutoRepositoryIT.TestBeans.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RedisAutoRepositoryIT extends BaseRedisRepositoryIT {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.2.5"))
                    .withExposedPorts(6379)
                    // 포트 리스닝 후 바로 핸드셰이크 닫힘 방지용
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    /**
     * 컨테이너가 매핑한 host/port를 Spring 환경 속성에 주입한다.
     * host/port를 제공하되, spring.redis.uri는 제공하지 않아 Auto 경로를 타게 함.
     */
    @DynamicPropertySource
    static void registerAutoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
        registry.add("spring.redis.enabled", () -> "true");
        registry.add("spring.redis.trusted-package", () -> "org.example.order");
        registry.add("spring.redis.client-name", () -> "order-core-it-auto");
    }

    @EnableConfigurationProperties(RedisProperties.class)
    static class TestBeans {
        @Bean
        public RedisRepository redisRepository(org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
            return new RedisRepositoryImpl(redisTemplate);
        }
    }
}
