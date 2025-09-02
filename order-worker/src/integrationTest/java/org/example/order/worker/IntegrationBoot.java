package org.example.order.worker;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * 통합테스트용 Boot
 * - 컴포넌트 스캔 없음 (외부 의존성 차단)
 * - Redis/Redisson 자동설정 제외
 */
@SpringBootConfiguration
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "org.redisson.spring.starter.RedissonAutoConfigurationV2",
        "org.redisson.spring.starter.RedissonReactiveAutoConfigurationV2"
})
public class IntegrationBoot {
}
