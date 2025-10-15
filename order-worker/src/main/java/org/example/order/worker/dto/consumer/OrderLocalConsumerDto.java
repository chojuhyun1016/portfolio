package org.example.order.worker.dto.consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.shared.op.Operation;

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

    public static OrderLocalConsumerDto from(OrderLocalMessage evt) {
        if (evt == null) {
            return null;
        }

        return new OrderLocalConsumerDto(evt.id(), evt.operation(), evt.publishedTimestamp());
    }

    public void validate() {
        if (id == null || operation == null || publishedTimestamp == null) {
            throw new IllegalArgumentException("OrderLocalConsumerDto missing required fields");
        }
    }
}
