package org.example.order.core.infra.jpa.repository.order.jpa.adapter;

import org.example.order.domain.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA Adapter (Infra)
 * - JpaRepository 기능만 담당
 */
public interface SpringDataOrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    void deleteByOrderIdIn(List<Long> orderId);
}
