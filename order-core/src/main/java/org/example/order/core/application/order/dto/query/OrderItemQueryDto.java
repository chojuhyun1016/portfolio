package org.example.order.core.application.order.dto.query;

import org.example.order.domain.order.entity.OrderEntity;

import java.time.LocalDateTime;

/**
 * 단일 주문 데이터 조회용 DTO (Application 계층)
 *
 * Query 계층에서 사용:
 * - 단일 주문 데이터의 상세 정보를 응답할 때 사용
 * - Domain Entity → DTO로 변환하는 정적 팩토리 메서드 포함
 */
public record OrderItemQueryDto(
        Long id,
        Long userId,
        String userNumber,
        Long orderId,
        String orderNumber,
        Long orderPrice,
        LocalDateTime publishDatetime,
        Boolean deleteYn,
        Long createdUserId,
        String createdUserType,
        LocalDateTime createdDatetime,
        Long modifiedUserId,
        String modifiedUserType,
        LocalDateTime modifiedDatetime,
        Long version
) {
    /**
     * Entity → Query DTO 변환 메서드
     *
     * @param entity 주문 엔티티
     * @return OrderItemQuery DTO
     */
    public static OrderItemQueryDto toDto(OrderEntity entity) {
        return new OrderItemQueryDto(
                entity.getId(),
                entity.getUserId(),
                entity.getUserNumber(),
                entity.getOrderId(),
                entity.getOrderNumber(),
                entity.getOrderPrice(),
                entity.getPublishedDatetime(),
                entity.getDeleteYn(),
                entity.getCreatedUserId(),
                entity.getCreatedUserType(),
                entity.getCreatedDatetime(),
                entity.getModifiedUserId(),
                entity.getModifiedUserType(),
                entity.getModifiedDatetime(),
                entity.getVersion()
        );
    }
}
