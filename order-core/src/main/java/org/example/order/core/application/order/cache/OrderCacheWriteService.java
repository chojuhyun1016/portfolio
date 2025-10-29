package org.example.order.core.application.order.cache;

import lombok.RequiredArgsConstructor;
import org.example.order.cache.feature.order.model.OrderCacheRecord;
import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.core.application.order.mapper.OrderCacheAssembler;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderCacheWriteService
 * ------------------------------------------------------------------------
 * 목적
 * - 캐시 쓰기/삭제 책임을 코어에서 통일 관리
 * - 상위 모듈(worker 등)이 캐시 레코드 타입에 직접 의존하지 않도록 캡슐화
 * - 라이브러리 모듈 스캔 배제를 위해 @Service 대신 @Bean 구성으로 제공
 */
@RequiredArgsConstructor
public class OrderCacheWriteService {

    private final OrderCacheRepository orderCacheRepository;

    @Transactional
    public void upsert(LocalOrderSync sync, Long ttlSeconds) {
        OrderCacheRecord rec = OrderCacheAssembler.from(sync);

        if (rec != null && rec.orderId() != null) {
            orderCacheRepository.put(rec, ttlSeconds);
        }
    }

    @Transactional
    public void evict(Long orderId) {
        if (orderId != null) {
            orderCacheRepository.evict(orderId);
        }
    }
}
