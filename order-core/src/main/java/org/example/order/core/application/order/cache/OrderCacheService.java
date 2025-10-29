package org.example.order.core.application.order.cache;

import lombok.RequiredArgsConstructor;
import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.core.application.order.dto.view.OrderView;
import org.example.order.core.application.order.mapper.OrderCacheViewMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OrderCacheService
 * - order-cache 레이어를 감싸는 코어 서비스
 * - 상위 모듈(API/Facade)은 캐시 모듈을 직접 의존하지 않고 본 서비스를 사용
 * - 라이브러리 모듈 스캔 배제를 위해 @Service 대신 @Bean 구성으로 제공
 */
@RequiredArgsConstructor
public class OrderCacheService {

    private final OrderCacheRepository orderCacheRepository;
    private final OrderCacheViewMapper cacheViewMapper;

    @Transactional(readOnly = true)
    public Optional<OrderView> getViewByOrderId(Long orderId) {
        return orderCacheRepository
                .get(orderId)
                .map(cacheViewMapper::toView);
    }
}
