package org.example.order.core.infra.persistence.order.jpa.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.entity.QOrderEntity;
import org.example.order.domain.order.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * OrderRepository 구현체 (JPA, EntityManager + QueryDSL)
 * <p>
 * - JpaInfraConfig 에서 jpa.enabled=true & JPAQueryFactory 존재 시 등록
 * - Spring Data 어댑터 제거: 단일 레이어(EM/QueryDSL)로 일관 구현
 */
@RequiredArgsConstructor
public class OrderRepositoryJpaImpl implements OrderRepository {

    private static final QOrderEntity ORDER = QOrderEntity.orderEntity;

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    @Override
    public Optional<OrderEntity> findById(Long id) {
        return Optional.ofNullable(em.find(OrderEntity.class, id));
    }

    @Override
    public void deleteByOrderIdIn(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }

        queryFactory
                .delete(ORDER)
                .where(ORDER.orderId.in(orderIds))
                .execute();

        // 대용량 삭제 후 영속성 컨텍스트 정합성 보장을 위해 필요 시 clear 고려
        // em.clear();
    }

    @Override
    public void save(OrderEntity entity) {
        if (entity == null) {
            return;
        }

        if (entity.getId() == null) {
            em.persist(entity);
        } else {
            em.merge(entity);
        }
    }
}
