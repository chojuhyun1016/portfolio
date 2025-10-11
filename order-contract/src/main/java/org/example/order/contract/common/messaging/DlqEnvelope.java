package org.example.order.contract.common.messaging;

/**
 * DLQ로 흘러가는 공통 Envelope (와이어 스키마)
 * - 처리 상태 필드(예: failedCount)는 제외
 * - type은 문자열(도메인별 enum 텍스트)로 단순화
 */
public record DlqEnvelope(
        String type,
        ErrorPayload error
) {
}
