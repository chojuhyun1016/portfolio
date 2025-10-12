package org.example.order.core.application.order.mapper;

import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;

import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.TimeProvider;

import org.mapstruct.*;

@Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class, TimeProvider.class},
        imports = {MessageOrderType.class}
)
public interface OrderMapper {

    // LocalOrderCommand -> OrderLocalMessage
    @Mapping(target = "id", source = "orderId")
    @Mapping(target = "category", expression = "java(MessageOrderType.ORDER_LOCAL)")
    @Mapping(
            target = "publishedTimestamp",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.localDateTimeToEpochMillis(timeProvider.now()))"
    )
    OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command, @Context TimeProvider timeProvider);

    default OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command) {
        return toOrderLocalMessage(command, new TimeProvider());
    }

    // Entity -> DTO
    LocalOrderDto toDto(OrderEntity entity);

    @AfterMapping
    default void setPublishedTimestamp(@MappingTarget LocalOrderDto dto, OrderEntity entity) {
        if (entity != null && entity.getPublishedDatetime() != null) {
            dto.updatePublishedTimestamp(
                    org.example.order.core.support.mapping.TimeMapper.localDateTimeToEpochMillis(entity.getPublishedDatetime())
            );
        }
    }

    // DTO -> Entity
    @ObjectFactory
    default OrderEntity newOrderEntity(LocalOrderDto dto) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(dto.getId());

        return e;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedDatetime", ignore = true)
    OrderEntity toEntity(LocalOrderDto dto);

    @AfterMapping
    default void fillEntity(@MappingTarget OrderEntity entity, LocalOrderDto dto) {
        entity.updateAll(
                dto.getUserId(),
                dto.getUserNumber(),
                dto.getOrderId(),
                dto.getOrderNumber(),
                dto.getOrderPrice(),
                dto.getDeleteYn(),
                dto.getVersion(),
                org.example.order.core.support.mapping.TimeMapper.epochMillisToLocalDateTime(dto.getPublishedTimestamp()),
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
    OrderUpdate toUpdate(LocalOrderDto dto);
}
