package org.example.order.contract.shared.error;

import java.util.Map;

/**
 * ErrorDetail (계약)
 * ------------------------------------------------------------
 * - 내부 예외타입/프레임워크 의존 없이 문자열/원시형만 사용
 * - stackTrace/meta는 선택(보안/크기 이슈 고려)
 */
public record ErrorDetail(
        String code,                // 표준 에러 코드 (예: "ORDER-001")
        String message,             // 에러 메시지
        String exception,           // 예외 클래스명
        Long occurredAtMs,          // 발생 시각(epoch millis)
        Map<String, String> meta,   // 부가 컨텍스트(필드/값 등) - 선택
        String stackTrace           // 스택트레이스(필요 시 truncate) - 선택
) {
}
