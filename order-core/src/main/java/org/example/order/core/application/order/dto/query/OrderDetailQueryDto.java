package org.example.order.core.application.order.dto.query;

import org.example.order.core.application.order.dto.internal.OrderEntityDto;

import java.time.LocalDateTime;

/**
 * 주문 단건 조회 응답용 DTO (읽기 전용)
 * - Domain/Entity를 알지 않음
 * - 내부 DTO에서 필요한 값만 투영
 */
public record OrderDetailQueryDto(
        Long id,
        Long userId,
        String userNumber,
        Long orderId,
        String orderNumber,
        Long orderPrice,
        Boolean deleteYn,
        LocalDateTime publishedDatetime,
        Long version
) {
    public static OrderDetailQueryDto from(OrderEntityDto dto) {
        if (dto == null) {
            return null;
        }

        return new OrderDetailQueryDto(
                dto.getId(),
                dto.getUserId(),
                dto.getUserNumber(),
                dto.getOrderId(),
                dto.getOrderNumber(),
                dto.getOrderPrice(),
                dto.getDeleteYn(),
                dto.getPublishedDatetime(),
                dto.getVersion()
        );
    }
}
