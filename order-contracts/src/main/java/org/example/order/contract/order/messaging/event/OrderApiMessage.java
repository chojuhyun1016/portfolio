package org.example.order.contract.order.messaging.event;

import org.example.order.contract.shared.op.Operation;
import org.example.order.contract.order.messaging.type.MessageOrderType;

import java.util.Locale;

/**
 * API 라우팅 메시지 계약
 * - Local 메시지를 API 경유/전달할 때 사용
 */
public record OrderApiMessage(
        Operation operation,
        MessageOrderType orderType,
        Long id,
        Long publishedTimestamp
) {
    public static OrderApiMessage fromLocal(OrderLocalMessage local) {
        if (local == null) {
            throw new IllegalArgumentException("local message is null");
        }

        // String -> Operation 안전 변환
        Operation op = parseOperation(local.getOperation());

        return new OrderApiMessage(
                op,
                MessageOrderType.ORDER_API,
                local.getId(),
                local.getPublishedTimestamp()
        );
    }

    /**
     * String 을 enum Operation 으로 안전 변환
     * - 앞뒤 공백 제거, 대문자 변환
     * - 실패 시 친절한 메시지로 IllegalArgumentException
     */
    private static Operation parseOperation(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("operation is required");
        }

        String norm = raw.trim().toUpperCase(Locale.ROOT);

        try {
            return Operation.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported operation: '" + raw + "'. " +
                    "Allowed: " + java.util.Arrays.toString(Operation.values()));
        }
    }
}
