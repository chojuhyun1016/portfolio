package org.example.order.core.application.order.mapper;

import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;

import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.TimeProvider;

import org.mapstruct.*;

import java.util.List;

/**
 * Order 스키마 전용 매퍼 (MapStruct)
 * - 공통 설정은 AppMappingConfig 에서 상속
 * - 시간 변환은 TimeMapper(@Named) 재사용
 */
@Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class, TimeProvider.class}
)
public interface OrderMapper {

    // ========= LocalOrderCommand -> OrderLocalMessage =========
    @Mapping(target = "id", source = "orderId")
    @Mapping(target = "publishedTimestamp",
            expression = "java(DateTimeUtils.localDateTimeToLong(timeProvider.now()))")
    OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command, @Context TimeProvider timeProvider);

    default OrderLocalMessage toOrderLocalMessage(LocalOrderCommand command) {
        return toOrderLocalMessage(command, new TimeProvider());
    }

    // ========= OrderEntity -> LocalOrderDto =========
    LocalOrderDto toDto(OrderEntity entity);

    @AfterMapping
    default void setPublishedTimestamp(@MappingTarget LocalOrderDto dto, OrderEntity entity) {
        if (entity != null && entity.getPublishedDatetime() != null) {
            dto.updatePublishedTimestamp(TimeMapper.toEpochMillis(entity.getPublishedDatetime()));
        }
    }

    // ========= LocalOrderDto -> OrderEntity =========
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
                TimeMapper.toLocalDateTime(dto.getPublishedTimestamp()),
                dto.getCreatedUserId(),
                dto.getCreatedUserType(),
                dto.getCreatedDatetime(),
                dto.getModifiedUserId(),
                dto.getModifiedUserType(),
                dto.getModifiedDatetime()
        );
    }

    // ========= LocalOrderDto -> OrderUpdate =========
    @Mapping(target = "publishedDateTime",
            source = "publishedTimestamp", qualifiedByName = "toLocalDateTime")
    OrderUpdate toUpdate(LocalOrderDto dto);

    // ========= List<LocalOrderDto> -> List<OrderUpdate> =========
    @IterableMapping(qualifiedByName = {})
    List<OrderUpdate> toUpdateCommands(List<LocalOrderDto> dtoList);
}
