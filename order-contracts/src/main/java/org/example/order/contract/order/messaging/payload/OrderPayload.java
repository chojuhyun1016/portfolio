package org.example.order.contract.order.messaging.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 메시지 페이로드 (주문 정보; 생성/수정 메타 포함)
 * - 내부 엔티티/DTO에 필요한 필드를 계약으로 그대로 전달
 * - null 허용(필요 케이스에서만 채움)
 */
public record OrderPayload(
        Long id,
        Long orderId,
        String orderNumber,
        Long userId,
        String userNumber,
        Long orderPrice,
        Boolean deleteYn,
        Long version,

        Long createdUserId,
        String createdUserType,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdDatetime,

        Long modifiedUserId,
        String modifiedUserType,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime modifiedDatetime,

        Long publishedTimestamp
) {
}
