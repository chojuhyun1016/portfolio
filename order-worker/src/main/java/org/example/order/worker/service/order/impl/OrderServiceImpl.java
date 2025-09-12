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

import java.util.List;

import org.example.order.common.support.logging.Correlate;

/**
 * OrderServiceImpl
 * ------------------------------------------------------------------------
 * 목적
 * - CRUD 실행 오케스트레이션.
 * MDC 전략
 * - 메시지가 강타입(OrderCrudMessage)으로 확정된 지점에서 첫 메시지의 도메인 키(orderId)를 traceId로 적용.
 * - @Correlate가 메서드 실행 전/후 MDC 백업/주입/복원을 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderCrudServiceImpl orderCrudService;

    // 상위 트랜잭션 실패에 상관없이 정상 일 때 커밋
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @Correlate(
            key = "#messages != null && #messages.size() > 0 ? #messages[0].dto.order.orderId : null",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void execute(MessageMethodType methodType, List<OrderCrudMessage> messages) {

        List<OrderDto> dtoList = messages.stream().map(OrderCrudMessage::getDto).toList();

        log.info("{}", dtoList);

        try {
            switch (methodType) {
                case POST -> orderCrudService.bulkInsert(dtoList.stream().map(OrderDto::getOrder).toList());
                case PUT -> orderCrudService.bulkUpdate(dtoList.stream().map(OrderDto::getOrder).toList());
                case DELETE -> orderCrudService.deleteAll(messages.stream().map(msg -> msg.getDto().getOrder()).toList());
            }
        } catch (Exception e) {
            log.error("error : execute order failed", e);
            log.error(e.getMessage(), e);

            throw e;
        }
    }
}