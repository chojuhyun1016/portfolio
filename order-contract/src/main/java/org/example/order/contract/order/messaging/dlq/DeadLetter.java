package org.example.order.contract.order.messaging.dlq;

import org.example.order.contract.shared.error.ErrorDetail;

/**
 * DLQ Envelope (계약)
 * ------------------------------------------------------------
 * - 실패 상태(예: failedCount) 같은 런타임 정책은 계약에서 제외
 * - type: 서비스 간 합의된 카테고리 문자열
 * - payload: 원본 메시지(또는 일부 요약)를 그대로 실어서 보내기 위함
 */
public record DeadLetter<T>(
        String type,
        ErrorDetail error,
        T payload
) {
    /**
     * Enum을 받아 type 문자열로 변환해주는 편의 팩토리
     */
    public static <T> DeadLetter<T> of(Enum<?> type, ErrorDetail error, T payload) {
        String t = (type == null ? null : type.name());

        return new DeadLetter<>(t, error, payload);
    }

    /**
     * 문자열 type을 직접 넘기는 팩토리
     */
    public static <T> DeadLetter<T> of(String type, ErrorDetail error, T payload) {
        return new DeadLetter<>(type, error, payload);
    }
}
