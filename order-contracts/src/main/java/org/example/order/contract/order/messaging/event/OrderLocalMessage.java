package org.example.order.contract.order.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.shared.op.Operation;

import java.util.Locale;

/**
 * 주문 로컬 메시지 (Contract)
 * - Producer/Consumer 공용 스키마
 * - 필수값 검증 메서드 제공: validation()
 * - operation은 계약 유연성(후방 호환)을 위해 String 유지
 */
@Getter
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PUBLIC) // MapStruct 생성자 접근 문제 해결
public class OrderLocalMessage {

    private final Long id;                    // 주문 ID (필수)
    private final String operation;           // 동작(예: CREATE/UPDATE/DELETE 등) (필수, String 유지)
    private final MessageOrderType orderType; // 메시지 타입 (예: ORDER_LOCAL) (필수)
    private final Long publishedTimestamp;    // 발행 시각(epoch millis) (필수)

    /**
     * 필수값 + Operation 유효성 검증
     */
    public void validation() throws IllegalArgumentException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OrderLocalMessage.id is required and must be positive.");
        }

        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("OrderLocalMessage.operation is required.");
        } else {
            String norm = operation.trim().toUpperCase(Locale.ROOT);

            try {
                Operation.valueOf(norm);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("OrderLocalMessage.operation unsupported value: '" + operation +
                        "'. Allowed: " + java.util.Arrays.toString(Operation.values()));
            }
        }

        if (orderType == null) {
            throw new IllegalArgumentException("OrderLocalMessage.orderType is required.");
        }

        if (publishedTimestamp == null || publishedTimestamp <= 0) {
            throw new IllegalArgumentException("OrderLocalMessage.publishedTimestamp is required and must be positive.");
        }
    }
}
