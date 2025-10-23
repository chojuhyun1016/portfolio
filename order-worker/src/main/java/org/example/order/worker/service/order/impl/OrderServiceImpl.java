package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.worker.dto.command.OrderCrudBatchCommand;
import org.example.order.worker.service.order.OrderService;
import org.example.order.common.support.logging.Correlate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
            key = "#batch != null && #batch.items() != null && #batch.items().size() == 1 ? #batch.items().get(0).orderId() : T(java.util.UUID).randomUUID().toString()",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void execute(OrderCrudBatchCommand batch) {
        final List<LocalOrderSync> safe = (batch == null || batch.items() == null) ? List.of() : batch.items();

        log.info("order-crud start: total={}, operation={}", safe.size(), (batch == null ? null : batch.operation()));

        safe.stream()
                .map(d -> d != null ? d.orderId() : null)
                .forEach(oid -> log.info("order-crud item: orderId={}, operation={}", oid, (batch == null ? null : batch.operation())));

        try {
            switch (batch.operation()) {
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
