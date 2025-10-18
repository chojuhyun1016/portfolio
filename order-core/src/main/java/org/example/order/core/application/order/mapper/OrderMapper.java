package org.example.order.core.application.order.mapper;

import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.core.application.order.dto.view.LocalOrderView;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.TimeProvider;
import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.mapstruct.*;

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
     * Entity <-> LocalOrderSync
     *  - publishedDatetime(LocalDateTime) <-> publishedTimestamp(Long)
     *  - (동기화/메시징 파이프라인에서 사용)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedTimestamp",
            source = "publishedDatetime",
            qualifiedByName = "localDateTimeToEpochMillis"
    )
    LocalOrderSync toDto(OrderEntity entity);

    @ObjectFactory
    default OrderEntity newOrderEntity(LocalOrderSync dto) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(dto.id());

        return e;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedDatetime", source = "publishedTimestamp", qualifiedByName = "epochMillisToLocalDateTime")
    OrderEntity toEntity(LocalOrderSync dto);

    /* ----------------------------------------------------------------------
     * LocalOrderSync -> OrderUpdate (Command)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedDateTime",
            source = "publishedTimestamp",
            qualifiedByName = "epochMillisToLocalDateTime"
    )
    OrderUpdate toUpdate(LocalOrderSync dto);

    java.util.List<OrderUpdate> toUpdateCommands(java.util.List<LocalOrderSync> dtos);

    /* ----------------------------------------------------------------------
     * LocalOrderSync -> LocalOrderView (선택적; 필요시 유지)
     * ---------------------------------------------------------------------- */
    @Mapping(target = "publishedTimestamp", source = "publishedTimestamp")
    @Mapping(target = "failure", source = "failure")
    LocalOrderView toView(LocalOrderSync dto);

    /* ----------------------------------------------------------------------
     * Entity -> LocalOrderView (조회 경로에서 Sync를 거치지 않음)
     * - publishedDatetime(LocalDateTime) -> publishedTimestamp(Long)
     * ---------------------------------------------------------------------- */
    @Mapping(
            target = "publishedTimestamp",
            source = "publishedDatetime",
            qualifiedByName = "localDateTimeToEpochMillis"
    )
    @Mapping(target = "failure", constant = "false")
    LocalOrderView toView(OrderEntity entity);
}
