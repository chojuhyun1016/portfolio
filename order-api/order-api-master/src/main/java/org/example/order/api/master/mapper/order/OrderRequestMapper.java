package org.example.order.api.master.mapper.order;

import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.springframework.stereotype.Component;

/**
 * API Request -> Application Command 변환 (수동 매퍼)
 * - 위치: API(어댑터) 레이어
 * - 의존 방향: adapter -> application (OK)
 */
@Component
public class OrderRequestMapper {

    public LocalOrderCommand toCommand(LocalOrderRequest req) {
        if (req == null) {
            return null;
        }

        return new LocalOrderCommand(req.orderId(), req.methodType());
    }
}
