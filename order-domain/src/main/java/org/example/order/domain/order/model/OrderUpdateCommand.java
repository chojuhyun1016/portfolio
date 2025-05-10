package org.example.order.domain.order.model;

import java.time.LocalDateTime;

/**
 * 도메인 전용 커맨드 모델 (레코드 기반)
 * - Core DTO에 의존하지 않고 Domain만 아는 데이터 구조
 */
public record OrderUpdateCommand(
        Long userId,
        String userNumber,
        Long orderId,
        String orderNumber,
        Long orderPrice,
        LocalDateTime publishedDateTime,
        Boolean deleteYn,
        Long createdUserId,
        String createdUserType,
        LocalDateTime createdDatetime,
        Long modifiedUserId,
        String modifiedUserType,
        LocalDateTime modifiedDatetime
) {}
