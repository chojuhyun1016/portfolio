package org.example.order.worker.service.common;

import org.example.order.core.application.order.dto.internal.OrderDto;

public interface OrderWebClientService {
    OrderDto findOrderListByOrderId(Long id);
}
