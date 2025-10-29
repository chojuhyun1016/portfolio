package org.example.order.cache.feature.order.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis 캐시 전용 Record
 * - 조회/키 인덱싱에 필요한 필드만 유지
 * - 상위(OrderView)로의 의존을 없애기 위한 캡슐화
 */
public record OrderCacheRecord(
        Long orderId,
        String orderNumber,
        Long userId,
        String userNumber,
        Long orderPrice,
        Boolean deleteYn,
        Long createdUserId,
        String createdUserType,
        LocalDateTime createdDatetime,
        Long modifiedUserId,
        String modifiedUserType,
        LocalDateTime modifiedDatetime,
        Long publishedTimestamp,
        Long versionStamp
) implements Serializable {
}
