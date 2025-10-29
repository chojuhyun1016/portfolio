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
 * 확장 방법
 * - 새로운 서비스가 생기면 해당 서비스 전용 Config를 만들고
 * 아래 @Import 리스트에 추가한다.
 * 예) PaymentMapperConfig.class (※ 요청에 따라 여긴 추가하지 않음)
 * <p>
 * 운영 가이드
 * - 필요 시 전체 애플리케이션 오토컨피그를 비활성화하려면:
 * order.application.enabled=false
 * (기본값: true)
 * <p>
 * 로딩 순서
 * - 캐시/레디스 오토컨피그 이후에 로드되어, 캐시 의존 빈들의 조건부 생성(@ConditionalOnBean)이
 * 안전하게 평가되도록 한다.
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.example.order.cache.autoconfig.CacheAutoConfiguration"
})
@ConditionalOnProperty(prefix = "order.application", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        OrderMapperConfig.class,
        OrderCacheConfig.class
})
public class ApplicationAutoConfiguration {
}
