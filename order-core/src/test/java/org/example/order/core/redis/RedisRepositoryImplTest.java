package org.example.order.core.redis;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.redis.repository.RedisRepository;
import org.example.order.core.redis.support.EmbeddedRedis;
import org.example.order.core.redis.support.EmbeddedRedisConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=" // 비밀번호 없음
})
@ContextConfiguration(classes = RedisRepositoryImplTest.TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisRepositoryImplTest {

    @Configuration
    @ComponentScan(basePackages = "org.example.order.core.redis")
    static class TestConfig {
        // 필요한 경우 Bean 직접 등록 가능
    }

    private EmbeddedRedis embeddedRedis;

    @Autowired
    RedisRepository redisRepository;

    @BeforeAll
    void setup() throws IOException {
        embeddedRedis = EmbeddedRedis.builder()
                .configuration(EmbeddedRedisConfiguration.builder().port(6379).build())
                .build();

        embeddedRedis.start();
        log.info("✅ Embedded Redis started.");
    }

    @AfterAll
    void teardown() {
        embeddedRedis.stop();
        log.info("🛑 Embedded Redis stopped.");
    }

    @Test
    void test_set_and_get_value() {
        // given
        String key = "test:key";
        String value = "Hello Redis";

        // when
        redisRepository.set(key, value);
        Object result = redisRepository.get(key);

        // then
        assertThat(result).isEqualTo(value);
    }

    @Test
    void test_hash_operations() {
        // given
        String hashKey = "test:hash";
        String field = "userId";
        String value = "123";

        // when
        redisRepository.putHash(hashKey, field, value);
        Object fetched = redisRepository.getHash(hashKey, field);

        // then
        assertThat(fetched).isEqualTo(value);
    }

    @Test
    void test_transaction_hash_put() {
        String hashKey = "trx:hash";
        Map<Object, Object> map = Map.of("field1", "v1", "field2", "v2");

        redisRepository.transactionPutAllHash(hashKey, map);

        assertThat(redisRepository.getHash(hashKey, "field1")).isEqualTo("v1");
    }
}
