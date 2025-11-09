package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.shared.op.Operation;
import org.example.order.worker.dto.consumer.OrderLocalConsumerDto;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * OrderLocalMessageFacadeImpl
 * - Local 컨슈머 DTO를 계약 메시지로 변환해 API 토픽에 전송
 * - 정상 전송 시 헤더를 수정하지 않음
 * - 실패 시 Envelope의 원본 헤더를 DLQ 전송에 사용
 * - 단건 처리 유지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageFacadeImpl implements OrderLocalMessageFacade {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public void sendOrderApiTopic(ConsumerEnvelope<OrderLocalConsumerDto> envelope) {
        OrderLocalConsumerDto dto = (envelope != null ? envelope.getPayload() : null);

        try {
            if (dto == null) {
                throw new IllegalArgumentException("OrderLocalConsumerDto is null");
            }

            dto.validate();

            final Operation op = normalizeOperationOrThrow(dto.getOperation());

            log.info("[LOCAL->API] orderId={} op={} ts={}",
                    dto.getId(), op, dto.getPublishedTimestamp());

            OrderApiMessage msg = new OrderApiMessage(
                    op,
                    MessageOrderType.ORDER_API,
                    dto.getId(),
                    dto.getPublishedTimestamp()
            );

            kafkaProducerService.sendToOrderApi(msg);
        } catch (Exception e) {
            log.error("order-local failed. orderId={} cause={}",
                    dto == null ? null : dto.getId(), e.toString(), e);

            kafkaProducerService.sendToDlq(dto, envelope != null ? envelope.getHeaders() : null, e);
        }
    }

    /**
     * operation 엄격 확정:
     * - null / blank -> 예외
     * - 문자열이면 대소문자 무시하고 Operation.valueOf 로만 허용 (alias/매핑 없음)
     * - 이미 enum 이면 그대로 반환
     * - 그 외 타입은 예외
     */
    private static Operation normalizeOperationOrThrow(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("operation is required");
        }

        if (raw instanceof Operation op) {
            return op;
        }

        if (raw instanceof CharSequence cs) {
            String norm = cs.toString().trim();

            if (norm.isEmpty()) {
                throw new IllegalArgumentException("operation is required");
            }

            try {
                return Operation.valueOf(norm.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Unsupported operation: '" + cs + "'. Allowed: " + java.util.Arrays.toString(Operation.values())
                );
            }
        }

        throw new IllegalArgumentException("operation type is invalid: " + raw.getClass().getName());
    }
}
