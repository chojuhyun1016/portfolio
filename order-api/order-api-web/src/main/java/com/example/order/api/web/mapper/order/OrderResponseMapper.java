package com.example.order.api.web.mapper.order;

import com.example.order.api.web.dto.order.OrderResponse;
import org.example.order.core.application.order.dto.internal.OrderSyncDto;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.springframework.stereotype.Component;

/**
 * Application DTO -> API Response 변환 매퍼 (수동)
 * - 위치: API(어댑터) 레이어
 * - 의존 방향: adapter -> application (OK)
 */
@Component
public class OrderResponseMapper {

    public OrderResponse toResponse(OrderDto dto) {
        if (dto == null) {
            return null;
        }

        OrderSyncDto o = dto.getOrder();

        if (o == null) {
            return null;
        }

        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getUserNumber(),
                o.getOrderId(),
                o.getOrderNumber(),
                o.getOrderPrice(),
                o.getDeleteYn(),
                o.getVersion(),
                o.getPublishedTimestamp()
        );
    }
}
