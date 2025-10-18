package org.example.order.api.master.mapper.order;

import org.example.order.api.master.dto.order.LocalOrderQueryResponse;
import org.example.order.core.application.order.dto.view.LocalOrderView;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.mapstruct.Mapper;

/**
 * Application 내부 View -> API 응답 DTO 매핑 (MapStruct)
 * - 계약(API) DTO는 여기서만 생성
 */
@Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class}
)
public interface OrderResponseMapper {

    LocalOrderQueryResponse toResponse(LocalOrderView view);
}
