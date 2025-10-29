package org.example.order.api.web.facade.order.impl;

import org.example.order.api.web.dto.order.OrderQueryResponse;
import org.example.order.api.web.facade.order.OrderQueryFacade;
import org.example.order.api.web.mapper.order.OrderResponseMapper;
import org.example.order.api.web.service.order.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.example.order.core.application.order.dto.query.OrderQuery;
import org.springframework.stereotype.Component;

/**
 * 파사드 구현
 * - 스토리지별 조회 위임
 */
@RequiredArgsConstructor
@Component
public class OrderQueryFacadeImpl implements OrderQueryFacade {

    private final OrderQueryService service;
    private final OrderResponseMapper responseMapper;

    @Override
    public OrderQueryResponse findByMySql(Long orderId) {
        var view = service.findByMySql(new OrderQuery(orderId));

        return responseMapper.toResponse(view);
    }

    @Override
    public OrderQueryResponse findByDynamo(Long orderId) {
        var view = service.findByDynamo(new OrderQuery(orderId));

        return responseMapper.toResponse(view);
    }

    @Override
    public OrderQueryResponse findByRedis(Long orderId) {
        var view = service.findByRedis(new OrderQuery(orderId));

        return responseMapper.toResponse(view);
    }
}
