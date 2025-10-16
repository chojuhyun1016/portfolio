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
 * - 날짜/시간 필드 매핑은 TimeMapper의 @Named 메서드로 명시 매핑
 */
@Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class, TimeProvider.class},
        imports = {MessageOrderType.class}
)
public interface OrderMapper {

    /* ----------------------------------------------------------------------
     * LocalOrderCommand -> OrderLocalMessage
     * ---------------------------------------------------------------------- */
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

    /* ----------------------------------------------------------------------
     * Entity -> DTO
     *  - publishedDatetime(LocalDateTime) -> publishedTimestamp(Long)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedTimestamp",
            source = "publishedDatetime",
            qualifiedByName = "localDateTimeToEpochMillis"
    )
    OrderSyncDto toDto(OrderEntity entity);

    /* ----------------------------------------------------------------------
     * DTO -> Entity
     *  - publishedTimestamp(Long) -> publishedDatetime(LocalDateTime)
     *  - id는 ObjectFactory에서 주입, 이후 매핑 단계에서는 ignore
     * ---------------------------------------------------------------------- */
    @ObjectFactory
    default OrderEntity newOrderEntity(OrderSyncDto dto) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(dto.getId());

        return e;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedDatetime", source = "publishedTimestamp", qualifiedByName = "epochMillisToLocalDateTime")
    OrderEntity toEntity(OrderSyncDto dto);

    /* ----------------------------------------------------------------------
     * DTO -> Update(Command)
     *  - publishedTimestamp(Long) -> publishedDateTime(LocalDateTime)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedDateTime",
            source = "publishedTimestamp",
            qualifiedByName = "epochMillisToLocalDateTime"
    )
    OrderUpdate toUpdate(OrderSyncDto dto);

    /* 배치 변환 */
    List<OrderUpdate> toUpdateCommands(List<OrderSyncDto> dtos);
}
