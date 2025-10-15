package org.example.order.core.application.order.mapper;

import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.OrderSyncDto;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.TimeProvider;
import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.mapstruct.*;

import java.util.List;

/**
 * OrderMapper
 * - DTO/Entity/메시지 간 매핑 정의
 */
@Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class, TimeProvider.class},
        imports = {MessageOrderType.class}
)
public interface OrderMapper {

    // LocalOrderCommand -> OrderLocalMessage
    @Mapping(target = "id", source = "orderId")
    @Mapping(target = "orderType", expression = "java(MessageOrderType.ORDER_LOCAL)")
    @Mapping(
            target = "publishedTimestamp",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.localDateTimeToEpochMillis(timeProvider.now()))"
    )
    OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command, @Context TimeProvider timeProvider);

    default OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command) {
        return toOrderLocalMessage(command, new TimeProvider());
    }

    // Entity -> DTO
    OrderSyncDto toDto(OrderEntity entity);

    @AfterMapping
    default void setPublishedTimestamp(@MappingTarget OrderSyncDto dto, OrderEntity entity) {
        if (entity != null && entity.getPublishedDatetime() != null) {
            dto.updatePublishedTimestamp(
                    TimeMapper.localDateTimeToEpochMillis(entity.getPublishedDatetime())
            );
        }
    }

    // DTO -> Entity
    @ObjectFactory
    default OrderEntity newOrderEntity(OrderSyncDto dto) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(dto.getId());

        return e;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedDatetime", ignore = true)
    OrderEntity toEntity(OrderSyncDto dto);

    @AfterMapping
    default void fillEntity(@MappingTarget OrderEntity entity, OrderSyncDto dto) {
        entity.updateAll(
                dto.getUserId(),
                dto.getUserNumber(),
                dto.getOrderId(),
                dto.getOrderNumber(),
                dto.getOrderPrice(),
                dto.getDeleteYn(),
                dto.getVersion(),
                TimeMapper.epochMillisToLocalDateTime(dto.getPublishedTimestamp()),
                dto.getCreatedUserId(),
                dto.getCreatedUserType(),
                dto.getCreatedDatetime(),
                dto.getModifiedUserId(),
                dto.getModifiedUserType(),
                dto.getModifiedDatetime()
        );
    }

    // DTO -> Update(Command)
    @Mapping(
            target = "publishedDateTime",
            source = "publishedTimestamp",
            qualifiedByName = "epochMillisToLocalDateTime"
    )
    OrderUpdate toUpdate(OrderSyncDto dto);

    // DTO(List) -> Update(List) : 배치 변환 (서비스에서 사용하는 toUpdateCommands)
    List<OrderUpdate> toUpdateCommands(List<OrderSyncDto> dtos);
}
