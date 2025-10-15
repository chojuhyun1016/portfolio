package org.example.order.worker.dto.consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.shared.op.Operation;

/**
 * OrderApiConsumerDto
 * 목적
 * - API 토픽에서 수신한 계약 이벤트를
 * worker 서비스 내부에서 사용하는 전용 DTO로 변환해 운반한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderApiConsumerDto {

    private Long id;
    private Operation operation;
    private Long publishedTimestamp;

    public static OrderApiConsumerDto from(OrderApiMessage evt) {
        if (evt == null) {
            return null;
        }

        return new OrderApiConsumerDto(evt.id(), evt.operation(), evt.publishedTimestamp());
    }

    public void validate() {
        if (id == null || operation == null || publishedTimestamp == null) {
            throw new IllegalArgumentException("OrderApiConsumerDto missing required fields");
        }
    }
}
