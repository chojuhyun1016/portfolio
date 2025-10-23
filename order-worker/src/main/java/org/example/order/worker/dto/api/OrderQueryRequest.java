package org.example.order.worker.dto.api;

/**
 * order-api-master /api/v1/local-orders/query 요청 바디
 */
public record OrderQueryRequest(Long orderId) {
}
