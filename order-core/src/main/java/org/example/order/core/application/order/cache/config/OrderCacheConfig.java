package org.example.order.core.application.order.cache.config;

import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.core.application.order.cache.OrderCacheService;
import org.example.order.core.application.order.cache.OrderCacheWriteService;
import org.example.order.core.application.order.cache.props.OrderCacheProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OrderCacheConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 코어 모듈에서 @Service/@Component 스캔에 의존하지 않도록
 * "캐시 관련" 필요한 빈들을 @Bean 팩토리 방식으로 제공한다.
 * - 상위 AutoConfig에서 포함되며, 캐시 사용 여부는 order.cache.* 토글로 제어한다.
 * <p>
 * 활성화 조건
 * - order.cache.enabled=true AND order.cache.redis.enabled=true
 * - 캐시 모듈 존재 및 저장소 빈 존재 시에만 실제 서비스 빈 생성.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = {
        "org.example.order.cache.autoconfig.RedisCacheAutoConfiguration"
})
@ConditionalOnExpression("'${order.cache.enabled:false}'=='true' && '${order.cache.redis.enabled:false}'=='true'")
@ConditionalOnClass(OrderCacheRepository.class)
@EnableConfigurationProperties(OrderCacheProperties.class)
public class OrderCacheConfig {

    @Bean
    @ConditionalOnBean(OrderCacheRepository.class)
    @ConditionalOnMissingBean
    public OrderCacheService orderCacheService(
            OrderCacheRepository repo,
            org.example.order.core.application.order.mapper.OrderCacheViewMapper mapper
    ) {
        return new OrderCacheService(repo, mapper);
    }

    @Bean
    @ConditionalOnBean(OrderCacheRepository.class)
    @ConditionalOnMissingBean
    public OrderCacheWriteService orderCacheWriteService(OrderCacheRepository repo,
                                                         OrderCacheProperties properties) {
        return new OrderCacheWriteService(repo, properties);
    }
}
