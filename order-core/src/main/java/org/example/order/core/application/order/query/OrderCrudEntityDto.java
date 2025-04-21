package org.example.order.core.application.order.query;

import org.example.order.core.domain.order.entity.OrderEntity;

import java.time.LocalDateTime;

public record OrderCrudEntityDto(
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
    public static OrderCrudEntityDto toDto(OrderEntity entity) {
        return new OrderCrudEntityDto(
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
