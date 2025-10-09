package org.example.order.api.master.service.order;

import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.OrderDto;

/**
 * Order Service
 * - 메시지 발행/전송
 * - 단건 조회(가공 포함)
 */
public interface OrderService {

    /**
     * Kafka로 메시지 전송
     */
    void sendMessage(LocalOrderCommand command);

    /**
     * 주문 단건 조회 (가공 포함)
     */
    OrderDto findById(Long id);
}
