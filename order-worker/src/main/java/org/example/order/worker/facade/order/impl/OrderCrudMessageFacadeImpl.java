package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.contract.order.messaging.event.OrderCloseMessage;
import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.internal.OrderSyncDto;
import org.example.order.worker.dto.consumer.OrderCrudConsumerDto;
import org.example.order.worker.exception.DatabaseExecuteException;
import org.example.order.worker.exception.WorkerExceptionCode;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.order.OrderService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OrderCrudMessageFacadeImpl
 * - CRUD 배치를 그룹핑하고 도메인 서비스에 위임
 * - 성공 건은 종료 메시지 발행, 실패 건은 호출측에서 DLQ 전송 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCrudMessageFacadeImpl implements OrderCrudMessageFacade {

    private final KafkaProducerService kafkaProducerService;
    private final OrderService orderService;

    @Transactional
    @Override
    public void executeOrderCrud(List<ConsumerEnvelope<OrderCrudConsumerDto>> envelopes) {
        if (ObjectUtils.isEmpty(envelopes)) {
            return;
        }

        log.debug("order-crud envelopes: {}", envelopes.size());

        // Envelope -> 내부 dto
        List<OrderCrudConsumerDto> dtos = envelopes.stream()
                .map(ConsumerEnvelope::getPayload)
                .toList();

        List<OrderSyncDto> failureList = new ArrayList<>();

        try {
            dtos.forEach(OrderCrudConsumerDto::validate);

            Map<Operation, List<OrderSyncDto>> byType = groupingByOperation(dtos);

            byType.forEach((operation, orders) -> {
                try {
                    orderService.execute(operation, orders);

                    for (OrderSyncDto order : orders) {
                        if (Boolean.TRUE.equals(order.getFailure())) {
                            log.info("failed order: {}", order);

                            failureList.add(order);
                        } else {
                            kafkaProducerService.sendToOrderRemote(
                                    OrderCloseMessage.of(order.getOrderId(), operation)
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("error: order crud execute failed. operation={}, orders={}", operation, orders, e);
                }
            });

            if (!failureList.isEmpty()) {
                throw new DatabaseExecuteException(WorkerExceptionCode.MESSAGE_UPDATE_FAILED);
            }
        } catch (DatabaseExecuteException e) {
            log.error("error: order crud database execute failed", e);

            throw e;
        } catch (Exception e) {
            log.error("error: order crud unknown exception", e);

            throw e;
        }
    }

    private Map<Operation, List<OrderSyncDto>> groupingByOperation(List<OrderCrudConsumerDto> dtos) {
        try {
            return dtos.stream()
                    .collect(Collectors.groupingBy(
                            OrderCrudConsumerDto::getOperation,
                            Collectors.mapping(OrderCrudConsumerDto::getOrder, Collectors.toList())
                    ));
        } catch (Exception e) {
            log.error("error: order crud dtos grouping failed: {}", dtos);
            log.error(e.getMessage(), e);

            throw new CommonException(WorkerExceptionCode.MESSAGE_GROUPING_FAILED);
        }
    }
}
