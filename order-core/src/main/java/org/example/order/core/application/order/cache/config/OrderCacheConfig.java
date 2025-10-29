package org.example.order.core.application.order.cache.config;

import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.core.application.order.mapper.OrderCacheViewMapper;
import org.example.order.core.application.order.cache.OrderCacheService;
import org.example.order.core.application.order.cache.OrderCacheWriteService;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OrderCacheConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 코어 모듈에서 @Service/@Component 스캔에 의존하지 않도록
 * "캐시 관련" 필요한 빈들을 @Bean 팩토리 방식으로 제공한다.
 * - 앱 모듈은 @Import(OrderCacheConfig.class) 하거나
 * 상위 AutoConfig에서 포함하면 된다.
 */
@Configuration(proxyBeanMethods = false)
public class OrderCacheConfig {

    /**
     * MapStruct Mapper
     * - 스캔 없이도 사용 가능하도록 팩토리에서 직접 생성
     */
    @Bean
    public OrderCacheViewMapper orderCacheViewMapper() {
        return Mappers.getMapper(OrderCacheViewMapper.class);
    }

    /**
     * 코어 서비스 빈들 (캐시 관련)
     * - 스캔 의존 제거: @Service 애노테이션 대신 @Bean 구성
     * - 캐시 모듈 비활성 환경에서도 실패하지 않도록 조건부 생성
     */
    @Bean
    @ConditionalOnBean(OrderCacheRepository.class)
    public OrderCacheService orderCacheService(OrderCacheRepository repo,
                                               OrderCacheViewMapper mapper) {
        return new OrderCacheService(repo, mapper);
    }

    @Bean
    @ConditionalOnBean(OrderCacheRepository.class)
    public OrderCacheWriteService orderCacheWriteService(OrderCacheRepository repo) {
        return new OrderCacheWriteService(repo);
    }
}
