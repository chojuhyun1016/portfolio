package org.example.order.core.infra.persistence.order.jpa.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.order.entity.LocalOrderEntity;
import org.example.order.domain.order.entity.QLocalOrderEntity;
import org.example.order.domain.order.repository.LocalOrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * LocalOrderRepository 구현체 (JPA, EntityManager + QueryDSL)
 * - 기존 OrderRepositoryJpaImpl과 동일한 스타일, 테이블만 local_order
 */
@RequiredArgsConstructor
public class LocalOrderRepositoryJpaImpl implements LocalOrderRepository {

    private static final QLocalOrderEntity LOCAL_ORDER = QLocalOrderEntity.localOrderEntity;

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    @Override
    public Optional<LocalOrderEntity> findById(Long id) {
        return Optional.ofNullable(em.find(LocalOrderEntity.class, id));
    }

    @Override
    public void deleteByOrderIdIn(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }

        queryFactory
                .delete(LOCAL_ORDER)
                .where(LOCAL_ORDER.orderId.in(orderIds))
                .execute();
    }

    @Override
    public void save(LocalOrderEntity entity) {
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
