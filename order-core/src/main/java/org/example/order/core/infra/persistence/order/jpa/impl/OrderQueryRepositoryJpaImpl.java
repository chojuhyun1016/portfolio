package org.example.order.core.infra.persistence.order.jpa.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.order.entity.QOrderEntity;
import org.example.order.domain.order.model.OrderView;
import org.example.order.domain.order.repository.OrderQueryRepository;

import java.util.Optional;

/**
 * OrderQueryRepository 구현체 (JPA QueryDSL)
 * <p>
 * - OrderInfraTestConfig (또는 InfraConfig) 에서
 * jpa.enabled=true && JPAQueryFactory 빈 존재 시 명시적으로 등록한다.
 */
@RequiredArgsConstructor
public class OrderQueryRepositoryJpaImpl implements OrderQueryRepository {

    private static final QOrderEntity ORDER = QOrderEntity.orderEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<OrderView> fetchByOrderId(Long orderId) {
        OrderView row = queryFactory
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
                .limit(1)
                .fetchFirst();

        return Optional.ofNullable(row);
    }
}
