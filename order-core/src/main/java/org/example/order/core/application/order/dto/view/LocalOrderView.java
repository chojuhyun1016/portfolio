package org.example.order.core.application.order.dto.view;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 조회 결과 View DTO (Application 계층 전용)
 * - API 응답과 1:1이 아님 (API는 전용 Response DTO 사용)
 * - 내부 유스케이스 결과를 표현
 */
@Getter
@Builder
public class LocalOrderView {

    private final Long id;

    private final Long userId;
    private final String userNumber;

    private final Long orderId;
    private final String orderNumber;
    private final Long orderPrice;

    private final Boolean deleteYn;
    private final Long version;

    private final Long createdUserId;
    private final String createdUserType;
    private final java.time.LocalDateTime createdDatetime;

    private final Long modifiedUserId;
    private final String modifiedUserType;
    private final java.time.LocalDateTime modifiedDatetime;

    private final Long publishedTimestamp;

    private final Boolean failure;
}
