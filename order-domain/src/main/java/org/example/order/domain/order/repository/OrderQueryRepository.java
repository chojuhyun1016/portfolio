package org.example.order.domain.order.repository;

import org.example.order.domain.order.model.OrderView;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 주문 조회/갱신 리포지토리 (QueryDSL/JPA)
 */
public interface OrderQueryRepository {

    /**
     * 주문 ID로 단건 프로젝션 조회
     */
    Optional<OrderView> fetchByOrderId(Long orderId);

    /**
     * 단일 주문 행을 orderId 기준으로 갱신한다.
     */
    int updateByOrderId(Long orderId,
                        String orderNumber,
                        Long userId,
                        String userNumber,
                        Long orderPrice,
                        Boolean deleteYn,
                        Long modifiedUserId,
                        String modifiedUserType,
                        LocalDateTime modifiedDatetime);
}
