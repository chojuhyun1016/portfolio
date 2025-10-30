package org.example.order.core.application.order.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.cache.feature.order.model.OrderCacheRecord;
import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.example.order.core.application.order.cache.props.OrderCacheProperties;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.core.application.order.mapper.OrderCacheAssembler;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderCacheWriteService
 * ------------------------------------------------------------------------
 * 목적
 * - 캐시 쓰기/삭제 책임을 코어에서 통일 관리
 * - 상위 모듈(worker 등)이 캐시 레코드 타입에 직접 의존하지 않도록 캡슐화
 * <p>
 * 개선사항
 * - 기본 TTL 외부화: order.application.cache.default-ttl-seconds
 * - upsert(sync) 오버로드 추가: 기본 TTL이 설정된 경우 이를 사용
 */
@Slf4j
@RequiredArgsConstructor
public class OrderCacheWriteService {

    private final OrderCacheRepository orderCacheRepository;
    private final OrderCacheProperties properties;

    /**
     * 명시 TTL로 업서트
     */
    @Transactional
    public void upsert(LocalOrderSync sync, Long ttlSeconds) {
        OrderCacheRecord rec = OrderCacheAssembler.from(sync);

        if (rec != null && rec.orderId() != null) {
            orderCacheRepository.put(rec, ttlSeconds);
        }
    }

    /**
     * 기본 TTL(프로퍼티)로 업서트
     * - default-ttl-seconds 가 설정되지 않았다면 아무 동작도 하지 않음(보수적 정책)
     */
    @Transactional
    public void upsert(LocalOrderSync sync) {
        Long defaultTtl = properties.getDefaultTtlSeconds();

        if (defaultTtl == null) {
            log.warn("[order-cache] default TTL is not set; skip upsert. (orderId={})",
                    sync != null ? sync.orderId() : null);

            return;
        }

        upsert(sync, defaultTtl);
    }

    @Transactional
    public void evict(Long orderId) {
        if (orderId != null) {
            orderCacheRepository.evict(orderId);
        }
    }
}
