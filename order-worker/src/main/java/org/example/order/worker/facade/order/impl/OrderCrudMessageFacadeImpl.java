package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.contract.order.messaging.event.OrderCloseMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OrderCrudMessageFacadeImpl
 * - CRUD 배치를 그룹핑하고 도메인 서비스에 위임
 * - 성공 건은 종료 메시지 발행
 * - 실패 건/예외는 Facade 단에서 원본 헤더 보존하여 DLQ로 전송
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

        try {
            List<OrderCrudConsumerDto> dtos = envelopes.stream()
                    .map(ConsumerEnvelope::getPayload)
                    .toList();

            Map<Long, ConsumerEnvelope<OrderCrudConsumerDto>> envelopeIndex =
                    envelopes.stream()
                            .filter(env -> env.getPayload() != null
                                    && env.getPayload().getOrder() != null
                                    && env.getPayload().getOrder().getOrderId() != null)
                            .collect(Collectors.toMap(
                                    env -> env.getPayload().getOrder().getOrderId(),
                                    Function.identity(),
                                    (a, b) -> b,
                                    LinkedHashMap::new
                            ));

            List<LocalOrderSync> failureList = new ArrayList<>();

            dtos.forEach(OrderCrudConsumerDto::validate);

            Map<Operation, List<LocalOrderSync>> byType = groupingByOperation(dtos);

            byType.forEach((operation, orders) -> {
                try {
                    orderService.execute(operation, orders);

                    for (LocalOrderSync order : orders) {
                        if (Boolean.TRUE.equals(order.getFailure())) {
                            log.info("failed order: {}", order);

                            sendOneToDlq(order, operation, envelopeIndex, new DatabaseExecuteException(WorkerExceptionCode.MESSAGE_UPDATE_FAILED));

                            failureList.add(order);
                        } else {
                            kafkaProducerService.sendToOrderRemote(
                                    OrderCloseMessage.of(order.getOrderId(), operation)
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("error: order crud execute failed. operation={}, orders={}", operation, orders, e);

                    sendGroupToDlq(orders, operation, envelopeIndex, e);

                    throw e;
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

            sendAllToDlq(envelopes, e);

            throw e;
        }
    }

    private Map<Operation, List<LocalOrderSync>> groupingByOperation(List<OrderCrudConsumerDto> dtos) {
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

    private void sendOneToDlq(LocalOrderSync order,
                              Operation operation,
                              Map<Long, ConsumerEnvelope<OrderCrudConsumerDto>> index,
                              Exception cause) {
        Long oid = (order == null ? null : order.getOrderId());
        ConsumerEnvelope<OrderCrudConsumerDto> env = (oid == null ? null : index.get(oid));

        if (env == null) {
            log.warn("skip dlq: envelope not found for orderId={}", oid);

            return;
        }

        OrderCrudMessage originalLike = OrderCrudMessage.of(operation, toPayload(order));

        kafkaProducerService.sendToDlq(originalLike, env.getHeaders(), cause);
    }

    private void sendGroupToDlq(List<LocalOrderSync> orders,
                                Operation operation,
                                Map<Long, ConsumerEnvelope<OrderCrudConsumerDto>> index,
                                Exception cause) {
        for (LocalOrderSync o : orders) {
            try {
                sendOneToDlq(o, operation, index, cause);
            } catch (Exception ex) {
                log.error("error: dlq send failed. orderId={}", (o == null ? null : o.getOrderId()), ex);
            }
        }
    }

    private void sendAllToDlq(List<ConsumerEnvelope<OrderCrudConsumerDto>> envelopes, Exception cause) {
        for (ConsumerEnvelope<OrderCrudConsumerDto> env : envelopes) {
            try {
                OrderCrudConsumerDto dto = env.getPayload();

                if (dto == null || dto.getOrder() == null) {
                    log.warn("skip dlq(all): empty payload");

                    continue;
                }

                OrderCrudMessage originalLike = OrderCrudMessage.of(dto.getOperation(), toPayload(dto.getOrder()));

                kafkaProducerService.sendToDlq(originalLike, env.getHeaders(), cause);
            } catch (Exception ex) {
                log.error("error: dlq send failed (all). key={}", env.getKey(), ex);
            }
        }
    }

    private OrderPayload toPayload(LocalOrderSync o) {
        if (o == null) {
            return null;
        }

        return new OrderPayload(
                o.getId(),
                o.getOrderId(),
                o.getOrderNumber(),
                o.getUserId(),
                o.getUserNumber(),
                o.getOrderPrice(),
                o.getDeleteYn(),
                o.getVersion(),
                o.getCreatedUserId(),
                o.getCreatedUserType(),
                o.getCreatedDatetime(),
                o.getModifiedUserId(),
                o.getModifiedUserType(),
                o.getModifiedDatetime(),
                o.getPublishedTimestamp()
        );
    }
}
