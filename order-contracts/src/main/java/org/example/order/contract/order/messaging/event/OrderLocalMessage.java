package org.example.order.contract.order.messaging.event;

import lombok.Builder;
import lombok.Getter;
import org.example.order.contract.order.messaging.type.MessageOrderType;

/**
 * 주문 로컬 메시지 (Contract)
 * - Producer/Consumer 공용 스키마
 * - 필수값 검증 메서드 제공: validation()
 */
@Getter
@Builder
public class OrderLocalMessage {

    private final Long id;                    // 주문 ID (필수)
    private final String operation;           // 동작(예: CREATE/UPDATE/DELETE 등) (필수)
    private final MessageOrderType orderType; // 메시지 타입 (예: ORDER_LOCAL) (필수)
    private final Long publishedTimestamp;    // 발행 시각(epoch millis) (필수)

    /**
     * 필수값 검증
     */
    public void validation() throws IllegalArgumentException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OrderLocalMessage.id is required and must be positive.");
        }

        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("OrderLocalMessage.operation is required.");
        }

        if (orderType == null) {
            throw new IllegalArgumentException("OrderLocalMessage.orderType is required.");
        }

        if (publishedTimestamp == null || publishedTimestamp <= 0) {
            throw new IllegalArgumentException("OrderLocalMessage.publishedTimestamp is required and must be positive.");
        }
    }
}
