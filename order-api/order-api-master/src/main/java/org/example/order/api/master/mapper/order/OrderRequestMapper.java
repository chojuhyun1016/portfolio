package org.example.order.api.master.mapper.order;

import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
import org.example.order.api.master.dto.order.LocalOrderQueryRequest;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.query.LocalOrderQuery;
import org.springframework.stereotype.Component;

/**
 * API Request -> Application Command/Query 변환 (수동 매퍼)
 * - 위치: API(어댑터) 레이어
 * - 의존 방향: adapter -> application (OK)
 */
@Component
public class OrderRequestMapper {

    public LocalOrderCommand toCommand(LocalOrderPublishRequest req) {
        if (req == null) {
            return null;
        }

        return new LocalOrderCommand(req.orderId(), req.operation());
    }

    public LocalOrderQuery toQuery(LocalOrderQueryRequest req) {
        if (req == null) {
            return null;
        }

        return new LocalOrderQuery(req.getOrderId());
    }
}
