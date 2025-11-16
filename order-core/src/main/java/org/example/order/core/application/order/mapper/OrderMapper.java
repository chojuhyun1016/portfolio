package org.example.order.core.application.order.mapper;

import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.core.application.order.dto.sync.OrderSync;
import org.example.order.core.application.order.dto.view.OrderView;
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
        imports = {MessageOrderType.class},
        builder = @Builder(disableBuilder = true)
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
     * Entity -> OrderSync / LocalOrderSync
     *  - publishedDatetime(LocalDateTime) -> publishedTimestamp(Long)
     *  - with* 메서드는 실제 속성이 아니므로 ignore 지정
     *  - failure는 기본 false
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedTimestamp",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.localDateTimeToEpochMillis(entity.getPublishedDatetime()))"
    )
    @Mapping(target = "failure", constant = "false")
    @Mapping(target = "withPublishedTimestamp", ignore = true)
    @Mapping(target = "withOrderNumber", ignore = true)
    @Mapping(target = "withOrderPrice", ignore = true)
    @Mapping(target = "withVersion", ignore = true)
    OrderSync toDto(OrderEntity entity);

    @Mapping(
            target = "publishedTimestamp",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.localDateTimeToEpochMillis(entity.getPublishedDatetime()))"
    )
    @Mapping(target = "failure", constant = "false")
    @Mapping(target = "withPublishedTimestamp", ignore = true)
    @Mapping(target = "withOrderNumber", ignore = true)
    @Mapping(target = "withOrderPrice", ignore = true)
    @Mapping(target = "withVersion", ignore = true)
    LocalOrderSync toLocalDto(OrderEntity entity);

    /* ----------------------------------------------------------------------
     * OrderSync / LocalOrderSync -> OrderEntity
     *  - id는 @ObjectFactory에서만 주입
     *  - publishedTimestamp(Long) -> publishedDatetime(LocalDateTime)
     * ---------------------------------------------------------------------- */
    @ObjectFactory
    default OrderEntity newOrderEntity(OrderSync dto) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(dto.id());

        return e;
    }

    @ObjectFactory
    default OrderEntity newOrderEntity(LocalOrderSync dto) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(dto.id());

        return e;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "publishedDatetime",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.epochMillisToLocalDateTime(dto.publishedTimestamp()))"
    )
    OrderEntity toEntity(OrderSync dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "publishedDatetime",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.epochMillisToLocalDateTime(dto.publishedTimestamp()))"
    )
    OrderEntity toEntity(LocalOrderSync dto);

    /* ----------------------------------------------------------------------
     * OrderSync / LocalOrderSync -> OrderUpdate (Command)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedDateTime",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.epochMillisToLocalDateTime(dto.publishedTimestamp()))"
    )
    OrderUpdate toUpdate(OrderSync dto);

    @Mapping(
            target = "publishedDateTime",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.epochMillisToLocalDateTime(dto.publishedTimestamp()))"
    )
    OrderUpdate toUpdate(LocalOrderSync dto);

    List<OrderUpdate> toUpdateCommands(List<LocalOrderSync> dtos);

    List<OrderUpdate> toUpdateCommandsFromOrderSync(List<OrderSync> dtos);

    /* ----------------------------------------------------------------------
     * OrderSync / LocalOrderSync -> OrderView
     * ---------------------------------------------------------------------- */
    @Mapping(target = "publishedTimestamp", source = "publishedTimestamp")
    @Mapping(target = "failure", source = "failure")
    OrderView toView(OrderSync dto);

    @Mapping(target = "publishedTimestamp", source = "publishedTimestamp")
    @Mapping(target = "failure", source = "failure")
    OrderView toView(LocalOrderSync dto);

    /* ----------------------------------------------------------------------
     * Entity -> OrderView  (조회 경로에서 Sync 생략)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedTimestamp",
            expression = "java(org.example.order.core.support.mapping.TimeMapper.localDateTimeToEpochMillis(entity.getPublishedDatetime()))"
    )
    @Mapping(target = "failure", constant = "false")
    OrderView toView(OrderEntity entity);
}
