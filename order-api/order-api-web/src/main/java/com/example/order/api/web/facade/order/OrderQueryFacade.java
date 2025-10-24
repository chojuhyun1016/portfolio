package com.example.order.api.web.facade.order;

import com.example.order.api.web.dto.order.OrderQueryResponse;

/**
 * Order Query Facade
 * - 스토리지 별( MySQL / Dynamo / Redis ) 조회 위임
 */
public interface OrderQueryFacade {

    OrderQueryResponse findByMySql(Long orderId);

    OrderQueryResponse findByDynamo(Long orderId);

    OrderQueryResponse findByRedis(Long orderId);
}
