package org.example.order.core.application.order.mapper;

import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.core.application.order.dto.command.OrderSyncCommandDto;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdateCommand;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OrderSyncCommandMapper
 * - DTO ↔ Entity 변환을 담당하는 Application 계층 Mapper
 * - DDD 원칙을 지키기 위해 Domain Entity는 DTO를 알 수 없으므로,
 *   DTO → Domain 변환은 반드시 Application 계층에서 수행해야 함
 */
@Component
public final class OrderSyncCommandMapper {

    /**
     * 기본 생성자 막기 (정적 유틸리티로도 일부 사용됨)
     */
    private OrderSyncCommandMapper() {}

    /**
     * OrderEntity → OrderSyncCommandDto 변환 (읽기용)
     *
     * @param entity OrderEntity (도메인 엔티티)
     * @return 변환된 OrderSyncCommandDto
     */
    public static OrderSyncCommandDto toCommand(OrderEntity entity) {
        if (entity == null) return null;

        return new OrderSyncCommandDto(
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
                DateTimeUtils.localDateTimeToLong(entity.getPublishedDatetime()), // UTC 기준 변환
                false // 기본 실패 상태: false
        );
    }

    /**
     * OrderSyncCommandDto → OrderEntity 변환 (쓰기용)
     *
     * @param dto OrderSyncCommandDto (Application Command DTO)
     * @return OrderEntity (Domain Entity)
     */
    public OrderEntity toEntity(OrderSyncCommandDto dto) {
        if (dto == null) return null;

        OrderEntity entity = OrderEntity.createEmpty(); // 생성자 보호된 경우 static factory 사용
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
     * OrderSyncCommandDto → OrderUpdateCommand 변환
     * - 도메인에 전달할 명령 객체로 변환
     *
     * @param dto OrderSyncCommandDto
     * @return OrderUpdateCommand (Domain Update Command)
     */
    public OrderUpdateCommand toUpdateCommand(OrderSyncCommandDto dto) {
        if (dto == null) return null;

        return new OrderUpdateCommand(
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
     * List<OrderSyncCommandDto> → List<OrderUpdateCommand> 변환
     *
     * @param dtoList OrderSyncCommandDto 리스트
     * @return 변환된 도메인 명령 리스트
     */
    public List<OrderUpdateCommand> toUpdateCommands(List<OrderSyncCommandDto> dtoList) {
        return dtoList == null ? List.of() :
                dtoList.stream()
                        .map(this::toUpdateCommand)
                        .toList();
    }
}
