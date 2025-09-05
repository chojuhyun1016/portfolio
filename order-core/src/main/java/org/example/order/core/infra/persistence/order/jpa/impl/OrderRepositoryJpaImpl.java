package org.example.order.core.infra.persistence.order.jpa.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.persistence.order.jpa.adapter.SpringDataOrderJpaRepository;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * OrderRepository 구현체 (JPA)
 * <p>
 * - JpaInfraConfig 에서 jpa.enabled=true & SpringDataOrderJpaRepository 존재 시 등록
 */
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderRepositoryJpaImpl implements OrderRepository {

    private final SpringDataOrderJpaRepository jpaRepository;

    @Override
    public Optional<OrderEntity> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteByOrderIdIn(List<Long> orderIds) {
        jpaRepository.deleteByOrderIdIn(orderIds);
    }

    @Override
    @Transactional
    public void save(OrderEntity entity) {
        jpaRepository.save(entity);
    }
}
