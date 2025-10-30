package org.example.order.core.application.config;

import org.example.order.core.application.order.cache.config.OrderCacheConfig;
import org.example.order.core.application.order.mapper.config.OrderMapperConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * ApplicationAutoConfiguration
 * ------------------------------------------------------------------------
 * 목적
 * - 애플리케이션 레이어 전반의 Auto-Config 집합 지점.
 * - 각 서비스별 구성(Order, ...)을 한 번에 등록한다.
 * <p>
 * 운영 가이드
 * - 전체 애플리케이션 오토컨피그 토글:
 * order.application.enabled=false (기본값: true)
 * - 캐시 사용 유무는 별도 토글:
 * order.cache.enabled=true/false, order.cache.redis.enabled=true/false
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.example.order.cache.autoconfig.RedisCacheAutoConfiguration"
})
@ConditionalOnProperty(prefix = "order.application", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        OrderMapperConfig.class,
        OrderCacheConfig.class
})
public class ApplicationAutoConfiguration {
}
