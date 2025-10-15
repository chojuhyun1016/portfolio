package org.example.order.worker.service.common;

import org.example.order.core.application.order.dto.internal.OrderSyncDto;

public interface WebClientService {
    OrderSyncDto findOrderListByOrderId(Long id);
}
