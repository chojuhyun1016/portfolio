package org.example.order.core.infra.redis;

import org.example.order.core.IntegrationBootApp; // ⬅️ 변경
import org.example.order.core.infra.redis.config.RedisTestSupport;
import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisTemplate 통합 테스트.
 */
@SpringBootTest(
        classes = IntegrationBootApp.class, // ⬅️ 변경
        properties = {
                "spring.profiles.active=integration",
                "lock.enabled=false",
                "lock.redisson.enabled=false",
                "lock.named.enabled=false"
        }
)
@Import(RedisTestSupport.class)
class RedisRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Test
    void set_and_get() {
        String key = "it:redis:key";
        String value = "hello";

        redisTemplate.opsForValue().set(key, value);
        Object got = redisTemplate.opsForValue().get(key);

        assertEquals(value, got);
    }
}
