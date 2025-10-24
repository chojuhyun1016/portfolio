package com.example.order.api.web.mapper.order;

import com.example.order.api.web.dto.order.OrderQueryResponse;
import org.example.order.core.application.order.dto.view.OrderView;
import org.example.order.core.support.mapping.TimeMapper;
import org.example.order.core.support.mapping.config.AppMappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Application 내부 View -> API 응답 DTO 매핑 (MapStruct)
 * - 계약(API) DTO는 여기서만 생성
 */
@Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class},
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderResponseMapper {

    OrderQueryResponse toResponse(OrderView view);
}
