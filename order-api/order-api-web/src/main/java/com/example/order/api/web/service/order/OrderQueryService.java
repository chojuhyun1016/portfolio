package com.example.order.api.web.service.order;

import org.example.order.core.application.order.dto.query.OrderQuery;
import org.example.order.core.application.order.dto.view.OrderView;

/**
 * Order Query Service
 * - 스토리지별 조회(MySQL/Dynamo/Redis)
 */
public interface OrderQueryService {

    OrderView findByMySql(OrderQuery query);

    OrderView findByDynamo(OrderQuery query);

    OrderView findByRedis(OrderQuery query);
}
