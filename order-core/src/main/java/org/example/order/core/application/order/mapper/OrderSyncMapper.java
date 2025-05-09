package org.example.order.core.application.order.mapper;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.core.application.order.dto.command.OrderSyncCommand;
import org.example.order.domain.order.entity.OrderEntity;

/**
 * OrderEntity ↔ OrderSyncCommand 변환 유틸 (Application 계층)
 */
public final class OrderSyncMapper {

    private OrderSyncMapper() {
    }

    /**
     * OrderEntity → OrderSyncCommand 변환
     *
     * @param entity OrderEntity (엔티티)
     * @return OrderSyncCommand (DTO)
     */
    public static OrderSyncCommand toCommand(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return new OrderSyncCommand(
                entity.getId(),
                entity.getUserId(),
                entity.getUserNumber(),
                entity.getOrderId(),
                entity.getOrderNumber(),
                entity.getOrderPrice(),
                entity.getDeleteYn(),
                entity.getVersion(),
                entity.getCreatedUserId(),
                entity.getCreatedUserType(),
                entity.getCreatedDatetime(),
                entity.getModifiedUserId(),
                entity.getModifiedUserType(),
                entity.getModifiedDatetime(),
                DateTimeUtils.localDateTimeToLong(entity.getPublishedDatetime()), // 변환 처리
                false // 초기 실패 상태
        );
    }

    /**
     * OrderSyncCommand → OrderEntity 변환
     *
     * @param command OrderSyncCommand (DTO)
     * @return OrderEntity (엔티티)
     */
    public static OrderEntity toEntity(OrderSyncCommand command) {
        if (command == null) {
            return null;
        }

        OrderEntity entity = OrderEntity.createEmpty();
        entity.setId(command.getId());

        entity.updateAll(
                command.getUserId(),
                command.getUserNumber(),
                command.getOrderId(),
                command.getOrderNumber(),
                command.getOrderPrice(),
                command.getDeleteYn(),
                command.getVersion(),
                DateTimeUtils.longToLocalDateTime(command.getPublishedTimestamp()), // 변환은 Application 계층에서 처리
                command.getCreatedUserId(),
                command.getCreatedUserType(),
                command.getCreatedDatetime(),
                command.getModifiedUserId(),
                command.getModifiedUserType(),
                command.getModifiedDatetime()
        );

        return entity;
    }
}
