package org.example.order.batch;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * 통합 테스트용 최소 부트스트랩.
 * - 컴포넌트 스캔 없음
 * - Redis/Redisson 자동 설정 제외
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
