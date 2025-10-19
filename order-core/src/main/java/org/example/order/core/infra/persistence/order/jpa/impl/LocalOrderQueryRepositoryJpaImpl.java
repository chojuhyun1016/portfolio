package org.example.order.core.infra.persistence.order.jpa.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.order.entity.QLocalOrderEntity;
import org.example.order.domain.order.model.OrderView;
import org.example.order.domain.order.repository.LocalOrderQueryRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * LocalOrderQueryRepository 구현체 (JPA QueryDSL)
 * - 기존 OrderQueryRepositoryJpaImpl과 동일한 로직, 엔티티만 LocalOrderEntity
 */
@RequiredArgsConstructor
public class LocalOrderQueryRepositoryJpaImpl implements LocalOrderQueryRepository {

    private static final QLocalOrderEntity LOCAL_ORDER = QLocalOrderEntity.localOrderEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<OrderView> fetchByOrderId(Long orderId) {
        OrderView row = queryFactory
                .select(Projections.constructor(
                        OrderView.class,
                        LOCAL_ORDER.orderId,
                        LOCAL_ORDER.orderNumber,
                        LOCAL_ORDER.userId,
                        LOCAL_ORDER.userNumber,
                        LOCAL_ORDER.orderPrice
                ))
                .from(LOCAL_ORDER)
                .where(LOCAL_ORDER.orderId.eq(orderId))
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
                .update(LOCAL_ORDER)
                .set(LOCAL_ORDER.userId, userId)
                .set(LOCAL_ORDER.userNumber, userNumber)
                .set(LOCAL_ORDER.orderPrice, orderPrice)
                .set(LOCAL_ORDER.deleteYn, deleteYn)
                .set(LOCAL_ORDER.modifiedUserId, modifiedUserId)
                .set(LOCAL_ORDER.modifiedUserType, modifiedUserType)
                .set(LOCAL_ORDER.modifiedDatetime, modifiedDatetime)
                .set(LOCAL_ORDER.orderNumber, orderNumber)
                .where(LOCAL_ORDER.orderId.eq(orderId))
                .execute();

        return (int) affected;
    }
}
