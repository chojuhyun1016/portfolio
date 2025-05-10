package org.example.order.core.infra.jpa.repository.order.jpa.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.jpa.repository.order.jpa.adapter.SpringDataOrderJpaRepository;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OrderRepository 구현체 (JPA)
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryJpaImpl implements OrderRepository {

    private final SpringDataOrderJpaRepository jpaRepository;

    @Override
    public Optional<OrderEntity> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void save(OrderEntity entity) {
        jpaRepository.save(entity);
    }

    @Override
    public void deleteByOrderIdIn(List<Long> orderIds) {
        jpaRepository.deleteByOrderIdIn(orderIds);
    }
}
