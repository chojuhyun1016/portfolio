package org.example.order.core.application.order.mapper;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OrderSyncCommandMapper
 * - LocalOrderDto ↔ OrderEntity ↔ OrderUpdateCommand 변환
 * - DDD 원칙에 따라 Entity는 DTO를 몰라야 하므로, 변환은 Application 계층에서 수행
 */
@Component
public final class OrderMapper {

    private OrderMapper() {}

    /**
     * OrderEntity → LocalOrderDto 변환
     */
    public static LocalOrderDto toDto(OrderEntity entity) {
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
