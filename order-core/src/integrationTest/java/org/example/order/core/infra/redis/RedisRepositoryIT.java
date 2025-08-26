package org.example.order.core.infra.redis;

import org.example.order.core.IntegrationBootApp; // ⬅️ 유지: 통합 테스트 전용 부트 루트
import org.example.order.core.infra.redis.config.RedisTestSupport;
import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// ✅ 추가: 자동설정 제외용
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisTemplate 통합 테스트.
 *
 * 🔧 수정 이유
 * - 클래스패스에 redisson-spring-boot-starter 가 존재하므로,
 *   단순 RedisTemplate 검증 테스트에서도 RedissonAutoConfigurationV2 가 자동으로 올라가
 *   localhost:6379 연결을 시도해 실패할 수 있음.
 *
 * ✅ 조치
 * - 이 테스트 컨텍스트에서만 Redis/Redisson 자동설정을 명시적으로 제외한다.
 *   (@ImportAutoConfiguration(exclude = …))
 * - Redis 연결 자체는 RedisTestSupport 가 제공하는 Testcontainers 기반 Lettuce 연결을 사용.
 */
@SpringBootTest(
        classes = IntegrationBootApp.class,
        properties = {
                "spring.profiles.active=integration",
                "lock.enabled=false",
                "lock.redisson.enabled=false",
                "lock.named.enabled=false"
        }
)
@Import(RedisTestSupport.class)
// ✅ 핵심: 레디슨/레디스 관련 자동설정 제외(이 테스트는 RedisTemplate만 검증)
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
        // 주의: RedisAutoConfiguration 은 RedisTestSupport 가 주입한 ConnectionFactory/Template를 사용하므로
        //       굳이 제외하지 않아도 되지만, 이미 직접 @Bean 으로 제공하므로 제외해도 무방합니다.
        //       필요 시 다음 라인도 함께 제외 가능합니다.
        // org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
})
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
