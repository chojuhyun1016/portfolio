package org.example.order.api.master.facade.order;

import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.core.application.order.dto.internal.OrderDto;

/**
 * Order Facade
 * - 메시지 발행/전송
 * - 단건 조회(가공 포함)
 */
public interface OrderFacade {

    void sendOrderMessage(LocalOrderRequest request);

    OrderDto findById(Long orderId);
}
