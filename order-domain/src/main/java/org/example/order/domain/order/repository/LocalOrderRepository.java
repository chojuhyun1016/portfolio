package org.example.order.domain.order.repository;

import org.example.order.domain.order.entity.LocalOrderEntity;

import java.util.List;
import java.util.Optional;

/**
 * LocalOrder 기본 Repository 인터페이스 (도메인)
 */
public interface LocalOrderRepository {
    Optional<LocalOrderEntity> findById(Long id);

    void save(LocalOrderEntity entity);

    void deleteByOrderIdIn(List<Long> orderIds);
}
