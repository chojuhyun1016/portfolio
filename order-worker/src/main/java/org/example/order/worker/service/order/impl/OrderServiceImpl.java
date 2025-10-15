package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.internal.OrderSyncDto;
import org.example.order.worker.service.order.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.example.order.common.support.logging.Correlate;

import java.util.List;

/**
 * OrderServiceImpl
 * ------------------------------------------------------------------------
 * 목적
 * - CRUD 실행 오케스트레이션
 * MDC 전략
 * - 메시지가 내부 DTO로 확정된 지점에서 도메인 키 기반 trace 세팅
 * - 단건이면 orderId, 그 외면 UUID를 traceId로 세팅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderCrudServiceImpl orderCrudService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @Correlate(
            key = "#messages != null && #messages.size() == 1 ? #messages[0].orderId : T(java.util.UUID).randomUUID().toString()",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void execute(Operation operation, List<OrderSyncDto> messages) {

        final List<OrderSyncDto> safe = (messages == null) ? List.of() : messages;

        log.info("order-crud start: total={}, operation={}", safe.size(), operation);

        safe.stream()
                .map(d -> d != null ? d.getOrderId() : null)
                .forEach(oid -> log.info("order-crud item: orderId={}, operation={}", oid, operation));

        try {
            switch (operation) {
                case CREATE -> orderCrudService.bulkInsert(safe);
                case UPDATE -> orderCrudService.bulkUpdate(safe);
                case DELETE -> orderCrudService.deleteAll(safe);
            }
        } catch (Exception e) {
            log.error("error: execute order failed", e);
            log.error(e.getMessage(), e);

            throw e;
        }
    }
}
