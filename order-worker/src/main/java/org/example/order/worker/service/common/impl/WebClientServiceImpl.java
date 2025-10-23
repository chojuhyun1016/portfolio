package org.example.order.worker.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.web.properties.WebUrlProperties;
import org.example.order.client.web.service.WebService;
import org.example.order.common.core.constant.HttpConstant;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.common.web.response.ApiResponse;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.worker.dto.api.OrderQueryRequest;
import org.example.order.worker.dto.api.OrderQueryResponse;
import org.example.order.worker.exception.WorkerExceptionCode;
import org.example.order.worker.service.common.WebClientService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebClientServiceImpl implements WebClientService {

    private final WebService webService;
    private final WebUrlProperties webUrlProperties;

    /**
     * order-api-master 로 POST /api/v1/local-orders/query 호출
     * - Body: { "orderId": <id> }
     * - Response: ApiResponse(data = LocalOrderQueryResponse)
     * - data 를 OrderQueryResponse -> LocalOrderSync 로 변환하여 반환
     */
    @Override
    public LocalOrderSync queryLocalOrderById(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("id is null");
            }

            WebUrlProperties.Client client = webUrlProperties.getClient();
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpConstant.X_CLIENT_ID, client.getClientId());

            final String base = client.getUrl().getOrder();
            final String url = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                    + "/api/v1/local-orders/query";

            OrderQueryRequest body = new OrderQueryRequest(id);

            log.info("[WebClientService] POST {}", url);

            @SuppressWarnings("unchecked")
            ApiResponse<?> response = (ApiResponse<?>) webService.post(url, headers, body, ApiResponse.class);

            Object data = (response == null ? null : response.getData());

            log.info("[WebClientService] response.data={}", data);

            OrderQueryResponse view = ObjectMapperUtils.convertTreeToValue(data, OrderQueryResponse.class);

            if (view == null) {
                throw new CommonException(WorkerExceptionCode.NOT_FOUND_LOCAL_RESOURCE, "empty data");
            }

            return view.toLocalOrderSync();
        } catch (Exception e) {
            log.error("error : not found local resource - id : {}", id);
            log.error("error : query local order failed", e);

            throw e;
        }
    }
}
