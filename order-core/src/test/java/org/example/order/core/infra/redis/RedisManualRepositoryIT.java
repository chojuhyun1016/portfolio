//package org.example.order.core.infra.redis;
//
//import org.example.order.core.infra.redis.config.RedisCommonConfig;
//import org.example.order.core.infra.redis.config.RedisManualConfig;
//import org.example.order.core.infra.redis.props.RedisProperties;
//import org.example.order.core.infra.redis.repository.RedisRepository;
//import org.example.order.core.infra.redis.repository.impl.RedisRepositoryImpl;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
///**
// * Manual 모드:
// * - spring.redis.enabled=true
// * - uri 존재 AND host/port 미설정
// */
//@SpringBootTest(classes = {
//        RedisManualConfig.class,
//        RedisCommonConfig.class,
//        RedisManualRepositoryIT.TestBeans.class
//})
//@Testcontainers
//@ActiveProfiles("it") // @Profile("!test") 조건 통과
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Import(RedisManualRepositoryIT.TestBeans.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//class RedisManualRepositoryIT extends BaseRedisRepositoryIT {
//
//    @Container
//    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0.5")
//            .withExposedPorts(6379)
//            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
//
//    @DynamicPropertySource
//    static void registerProps(DynamicPropertyRegistry registry) {
//        String uri = String.format("redis://%s:%d/0", redis.getHost(), redis.getMappedPort(6379));
//        registry.add("spring.redis.enabled", () -> "true");
//        registry.add("spring.redis.uri", () -> uri);
//        // host/port 미설정으로 Manual 조건 유도
//        registry.add("spring.redis.host", () -> "");
//        registry.add("spring.redis.port", () -> "");
//        registry.add("spring.redis.trusted-package", () -> "org.example.order");
//        registry.add("spring.redis.client-name", () -> "order-core-it-manual");
//    }
//
//    @EnableConfigurationProperties(RedisProperties.class)
//    static class TestBeans {
//        @Bean
//        public RedisRepository redisRepository(org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
//            return new RedisRepositoryImpl(redisTemplate);
//        }
//    }
//}
