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
import org.example.order.worker.dto.command.OrderCreateBatchCommand;
import org.example.order.worker.dto.command.OrderCrudBatchCommand;
import org.example.order.worker.dto.command.OrderDeleteBatchCommand;
import org.example.order.worker.dto.command.OrderUpdateBatchCommand;
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

            // 동일 orderId에 대한 모든 Envelope를 유지하는 멀티맵
            Map<Long, List<ConsumerEnvelope<OrderCrudConsumerDto>>> envelopeIndex =
                    envelopes.stream()
                            .filter(env -> env.getPayload() != null
                                    && env.getPayload().getOrder() != null
                                    && env.getPayload().getOrder().orderId() != null)
                            .collect(Collectors.groupingBy(
                                    env -> env.getPayload().getOrder().orderId(),
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            dtos.forEach(OrderCrudConsumerDto::validate);

            Map<Operation, List<LocalOrderSync>> byType = groupingByOperation(dtos);
            List<OrderCrudBatchCommand> batches = byType.entrySet().stream()
                    .map(e -> toBatch(e.getKey(), List.copyOf(e.getValue())))
                    .toList();

            List<LocalOrderSync> failureList = new ArrayList<>();

            for (OrderCrudBatchCommand batch : batches) {
                try {
                    orderService.execute(batch);

                    for (LocalOrderSync order : batch.items()) {
                        if (Boolean.TRUE.equals(order.failure())) {
                            log.info("failed order: {}", order);

                            sendOneToDlq(order, batch.operation(), envelopeIndex,
                                    new DatabaseExecuteException(WorkerExceptionCode.MESSAGE_UPDATE_FAILED));

                            failureList.add(order);
                        } else {
                            kafkaProducerService.sendToOrderRemote(
                                    OrderCloseMessage.of(order.orderId(), batch.operation())
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("error: order crud execute failed. operation={}, orders={}",
                            batch.operation(), batch.items(), e);

                    sendGroupToDlq(batch.items(), batch.operation(), envelopeIndex, e);

                    throw e;
                }
            }

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

    // 반환 타입을 인터페이스로 고정하는 팩토리 메서드
    private static OrderCrudBatchCommand toBatch(Operation op, List<LocalOrderSync> items) {
        return switch (op) {
            case CREATE -> new OrderCreateBatchCommand(items);
            case UPDATE -> new OrderUpdateBatchCommand(items);
            case DELETE -> new OrderDeleteBatchCommand(items);
        };
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
                              Map<Long, List<ConsumerEnvelope<OrderCrudConsumerDto>>> index,
                              Exception cause) {
        Long oid = (order == null ? null : order.orderId());
        List<ConsumerEnvelope<OrderCrudConsumerDto>> envs = (oid == null ? null : index.get(oid));

        if (envs == null || envs.isEmpty()) {
            log.warn("skip dlq: envelope not found for orderId={}", oid);

            return;
        }

        OrderCrudMessage originalLike = OrderCrudMessage.of(operation, toPayload(order));

        for (ConsumerEnvelope<OrderCrudConsumerDto> env : envs) {
            try {
                kafkaProducerService.sendToDlq(originalLike, env.getHeaders(), cause);
            } catch (Exception ex) {
                log.error("error: dlq send failed. orderId={}", oid, ex);
            }
        }
    }

    private void sendGroupToDlq(List<LocalOrderSync> orders,
                                Operation operation,
                                Map<Long, List<ConsumerEnvelope<OrderCrudConsumerDto>>> index,
                                Exception cause) {
        for (LocalOrderSync o : orders) {
            try {
                sendOneToDlq(o, operation, index, cause);
            } catch (Exception ex) {
                log.error("error: dlq send failed. orderId={}", (o == null ? null : o.orderId()), ex);
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
                o.id(),
                o.orderId(),
                o.orderNumber(),
                o.userId(),
                o.userNumber(),
                o.orderPrice(),
                o.deleteYn(),
                o.version(),
                o.createdUserId(),
                o.createdUserType(),
                o.createdDatetime(),
                o.modifiedUserId(),
                o.modifiedUserType(),
                o.modifiedDatetime(),
                o.publishedTimestamp()
        );
    }
}
