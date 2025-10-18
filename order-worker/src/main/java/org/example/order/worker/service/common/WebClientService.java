package org.example.order.worker.service.common;

import org.example.order.core.application.order.dto.sync.LocalOrderSync;

public interface WebClientService {
    LocalOrderSync findOrderListByOrderId(Long id);
}
