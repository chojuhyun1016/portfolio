package org.example.order.core.application.order.mapper;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderSyncCommandMapper
 */
@Component
public final class OrderMapper {

    private OrderMapper() {}

    /**
     * LocalOrderCommand → OrderLocalMessage 변환
     */
    public OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command) {
        if (command == null) return null;

        Long nowEpochMillis = DateTimeUtils.localDateTimeToLong(LocalDateTime.now());

        return new OrderLocalMessage(
                command.orderId(),
                command.methodType(),
                nowEpochMillis
        );
    }

    /**
     * OrderEntity → LocalOrderDto 변환
     */
    public LocalOrderDto toDto(OrderEntity entity) {
        if (entity == null) return null;

        return new LocalOrderDto(
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
                DateTimeUtils.localDateTimeToLong(entity.getPublishedDatetime()),
                false
        );
    }

    /**
     * LocalOrderDto → OrderEntity 변환
     */
    public OrderEntity toEntity(LocalOrderDto dto) {
        if (dto == null) return null;

        OrderEntity entity = OrderEntity.createEmpty();
        entity.setId(dto.getId());

        entity.updateAll(
                dto.getUserId(),
                dto.getUserNumber(),
                dto.getOrderId(),
                dto.getOrderNumber(),
                dto.getOrderPrice(),
                dto.getDeleteYn(),
                dto.getVersion(),
                DateTimeUtils.longToLocalDateTime(dto.getPublishedTimestamp()),
                dto.getCreatedUserId(),
                dto.getCreatedUserType(),
                dto.getCreatedDatetime(),
                dto.getModifiedUserId(),
                dto.getModifiedUserType(),
                dto.getModifiedDatetime()
        );

        return entity;
    }

    /**
     * LocalOrderDto → OrderUpdateCommand 변환
     */
    public OrderUpdate toUpdateCommand(LocalOrderDto dto) {
        if (dto == null) return null;

        return new OrderUpdate(
                dto.getUserId(),
                dto.getUserNumber(),
                dto.getOrderId(),
                dto.getOrderNumber(),
                dto.getOrderPrice(),
                DateTimeUtils.longToLocalDateTime(dto.getPublishedTimestamp()),
                dto.getDeleteYn(),
                dto.getCreatedUserId(),
                dto.getCreatedUserType(),
                dto.getCreatedDatetime(),
                dto.getModifiedUserId(),
                dto.getModifiedUserType(),
                dto.getModifiedDatetime()
        );
    }

    /**
     * List<LocalOrderDto> → List<OrderUpdateCommand> 변환
     */
    public List<OrderUpdate> toUpdateCommands(List<LocalOrderDto> dtoList) {
        return dtoList == null ? List.of() :
                dtoList.stream()
                        .map(this::toUpdateCommand)
                        .toList();
    }
}
