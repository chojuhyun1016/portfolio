package org.example.order.worker.service.common;

import org.example.order.core.application.order.dto.model.OrderDataModelDto;

public interface OrderWebClientService {
    OrderDataModelDto findOrderListByOrderId(Long id);
}
