package org.example.order.core.infra.persistence.order.jpa.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.order.entity.QOrderEntity;
import org.example.order.domain.order.model.OrderView;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OrderQueryRepository 구현체 (JPA QueryDSL)
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

    @Override
    @Transactional
    public int updateByOrderId(Long orderId,
                               String orderNumber,
                               Long userId,
                               String userNumber,
                               Long orderPrice,
                               Boolean deleteYn,
                               Long modifiedUserId,
                               String modifiedUserType,
                               LocalDateTime modifiedDatetime) {

        long affected = queryFactory
                .update(ORDER)
                .set(ORDER.userId, userId)
                .set(ORDER.userNumber, userNumber)
                .set(ORDER.orderPrice, orderPrice)
                .set(ORDER.deleteYn, deleteYn)
                .set(ORDER.modifiedUserId, modifiedUserId)
                .set(ORDER.modifiedUserType, modifiedUserType)
                .set(ORDER.modifiedDatetime, modifiedDatetime)
                .set(ORDER.orderNumber, orderNumber)
                .where(ORDER.orderId.eq(orderId))
                .execute();

        return (int) affected;
    }
}
