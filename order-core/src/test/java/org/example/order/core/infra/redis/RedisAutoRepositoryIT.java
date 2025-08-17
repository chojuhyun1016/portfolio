//package org.example.order.core.infra.redis;
//
//import org.example.order.core.infra.redis.config.RedisAutoConfig;
//import org.example.order.core.infra.redis.config.RedisCommonConfig;
//import org.example.order.core.infra.redis.props.RedisProperties;
//import org.example.order.core.infra.redis.repository.RedisRepository;
//import org.example.order.core.infra.redis.repository.impl.RedisRepositoryImpl;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//
//@SpringBootTest(
//        classes = {
//                RedisAutoConfig.class,
//                RedisCommonConfig.class,
//                RedisAutoRepositoryIT.TestBeans.class
//        },
//        properties = {
//                "spring.redis.enabled=true",
//                "spring.redis.trusted-package=org.example.order",
//                "spring.redis.client-name=order-core-it-auto"
//        }
//)
//@Testcontainers
//@ActiveProfiles("it")
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Import(RedisAutoRepositoryIT.TestBeans.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//class RedisAutoRepositoryIT extends BaseRedisRepositoryIT {
//
//    @Container
//    @ServiceConnection
//    static final GenericContainer<?> redis =
//            new GenericContainer<>(DockerImageName.parse("redis:7.2.5"))
//                    .withExposedPorts(6379)
//                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
//
//    @EnableConfigurationProperties(RedisProperties.class)
//    static class TestBeans {
//        @Bean
//        public RedisRepository redisRepository(org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
//            return new RedisRepositoryImpl(redisTemplate);
//        }
//    }
//}
