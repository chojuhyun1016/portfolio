package org.example.order.worker.service.common;

import org.example.order.core.application.dto.OrderDto;

public interface OrderWebClientService {
    OrderDto findOrderListByOrderId(Long id);
}
