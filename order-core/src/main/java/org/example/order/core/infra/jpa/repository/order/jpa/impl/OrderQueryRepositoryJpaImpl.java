package org.example.order.core.infra.jpa.repository.order.jpa.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.order.entity.QOrderEntity;
import org.example.order.domain.order.model.OrderView;
import org.example.order.domain.order.repository.OrderQueryRepository;

/**
 * OrderQueryRepository 구현체 (JPA QueryDSL)
 * <p>
 * - JpaInfraConfig 에서 jpa.enabled=true & JPAQueryFactory 존재 시 등록
 */
@RequiredArgsConstructor
public class OrderQueryRepositoryJpaImpl implements OrderQueryRepository {

    private static final QOrderEntity ORDER = QOrderEntity.orderEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public OrderView fetchByOrderId(Long orderId) {
        return queryFactory
                .select(Projections.constructor(
                        OrderView.class,
                        ORDER.orderId,
                        ORDER.orderNumber,
                        ORDER.userId,
                        ORDER.userNumber,
                        ORDER.orderPrice
                ))
                .from(ORDER)
                .where(ORDER.orderId.eq(orderId))
                .fetchOne();
    }
}
