package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
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
 * - CRUD 실행 오케스트레이션.
 * MDC 전략
 * - 메시지가 강타입(OrderCrudMessage)으로 확정된 지점에서 도메인 키 기반 trace 세팅.
 * - 단건이면 orderId, 그 외(0건/배치)면 UUID를 traceId로 세팅한다.
 * - @Correlate가 메서드 실행 전/후 MDC 백업/주입/복원을 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderCrudServiceImpl orderCrudService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @Correlate(
            key = "#messages != null && #messages.size() == 1 ? #messages[0].dto.order.orderId : T(java.util.UUID).randomUUID().toString()",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void execute(MessageMethodType methodType, List<OrderCrudMessage> messages) {

        final List<OrderCrudMessage> safe = (messages == null) ? List.of() : messages;

        log.info("order-crud start: total={}, method={}", safe.size(), methodType);

        safe.stream()
                .map(msg -> msg.getDto() != null && msg.getDto().getOrder() != null
                        ? msg.getDto().getOrder().getOrderId()
                        : null)
                .forEach(oid -> log.info("order-crud item: orderId={}, method={}", oid, methodType));

        List<OrderDto> dtoList = safe.stream().map(OrderCrudMessage::getDto).toList();

        try {
            switch (methodType) {
                case POST -> orderCrudService.bulkInsert(dtoList.stream().map(OrderDto::getOrder).toList());
                case PUT -> orderCrudService.bulkUpdate(dtoList.stream().map(OrderDto::getOrder).toList());
                case DELETE -> orderCrudService.deleteAll(
                        safe.stream().map(msg -> msg.getDto().getOrder()).toList()
                );
            }
        } catch (Exception e) {
            log.error("error : execute order failed", e);
            log.error(e.getMessage(), e);

            throw e;
        }
    }
}
