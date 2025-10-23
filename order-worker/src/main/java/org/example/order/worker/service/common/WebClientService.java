package org.example.order.worker.service.common;

import org.example.order.core.application.order.dto.sync.LocalOrderSync;

public interface WebClientService {
    /**
     * order-api-master에 LocalOrder 단건 조회 요청(POST /api/v1/local-orders/query).
     *
     * @param id 주문 ID
     * @return LocalOrderSync (내부에서 응답 DTO -> 내부 DTO 매핑)
     */
    LocalOrderSync queryLocalOrderById(Long id);
}
