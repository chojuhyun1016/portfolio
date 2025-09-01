package org.example.order.worker.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.web.config.property.WebClientUrlProperties;
import org.example.order.client.web.service.WebClientService;
import org.example.order.common.core.constant.HttpConstant;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.common.web.response.ApiResponse;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.worker.exception.WorkerExceptionCode;
import org.example.order.worker.service.common.OrderWebClientService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({WebClientUrlProperties.class})
public class OrderWebClientServiceImpl implements OrderWebClientService {

    private final WebClientService webClientService;
    private final WebClientUrlProperties webClientUrlProperties;

    @Override
    public OrderDto findOrderListByOrderId(Long id) {
        try {
            WebClientUrlProperties.Client client = webClientUrlProperties.getClient();

            Map<String, String> headers = new HashMap<>();
            headers.put(HttpConstant.X_CLIENT_ID, client.getClientId());

            String orderUrl = client.getUrl().getWithPathVariable(client.getUrl().getOrder(), id);
            log.info("{}", orderUrl);

            ApiResponse<?> response = (ApiResponse<?>) webClientService.get(orderUrl, headers, null, ApiResponse.class);
            Object data = response.getData();
            log.info("{}", response.getData());

            OrderDto result = ObjectMapperUtils.convertTreeToValue(data, OrderDto.class);

            if (result == null) {
                throw new CommonException(WorkerExceptionCode.NOT_FOUND_LOCAL_RESOURCE);
            }

            return result;
        } catch (Exception e) {
            log.error("error : not found local resource - id : {}", id);
            log.error("error : find order failed", e);

            throw e;
        }
    }
}
