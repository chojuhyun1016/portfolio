package org.example.order.worker.mapper.local;

import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.shared.op.Operation;
import org.example.order.worker.dto.consumer.OrderLocalConsumerDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * OrderLocalMessageMapper
 * - 계약 이벤트(OrderLocalMessage) -> 내부 DTO(OrderLocalConsumerDto) 변환
 * - 테스트/재사용 용이성 확보 목적
 * - Mapper vs Converter:
 * - 문자열 Enum 파싱 정도의 "가벼운 규칙 변환"은 Mapper에 포함
 */
@Component
public class OrderLocalMessageMapper {

    public OrderLocalConsumerDto toDto(OrderLocalMessage evt) {
        if (evt == null) {
            return null;
        }

        Operation op = null;
        if (evt.getOperation() != null && !evt.getOperation().isBlank()) {
            try {
                op = Operation.valueOf(evt.getOperation().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported operation value: " + evt.getOperation(), ex);
            }
        }

        return new OrderLocalConsumerDto(
                evt.getId(),
                op,
                evt.getPublishedTimestamp()
        );
    }
}
