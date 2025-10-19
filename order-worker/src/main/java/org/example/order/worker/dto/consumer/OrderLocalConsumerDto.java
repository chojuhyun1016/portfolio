package org.example.order.worker.dto.consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.shared.op.Operation;

import java.util.Locale;

/**
 * OrderLocalConsumerDto
 * 목적
 * - Local 토픽에서 수신한 계약 이벤트를
 * worker 서비스 내부에서 사용하는 전용 DTO로 변환해 운반한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderLocalConsumerDto {

    private Long id;
    private Operation operation;
    private Long publishedTimestamp;

    /**
     * 계약 이벤트 → 내부 전용 DTO 변환
     * - operation(String)을 Operation enum으로 안전 변환
     */
    public static OrderLocalConsumerDto from(OrderLocalMessage evt) {
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

    /**
     * 필수 필드 검증
     */
    public void validate() {
        if (id == null || operation == null || publishedTimestamp == null) {
            throw new IllegalArgumentException("OrderLocalConsumerDto missing required fields");
        }
    }
}
