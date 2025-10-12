package org.example.order.contract.common.monitoring.message;

/**
 * 모니터링 메시지 "계약" (카프카 등 외부 시스템으로 전송되는 와이어 스키마)
 * - 내부 공통/예외/enum 타입 의존 없음
 * - 필요한 "원시 필드"만 보유
 */
public record MonitoringMessage(
        int type,          // MonitoringType.code()
        int level,         // MonitoringSeverity.level()
        String company,    // MonitoringContext.COMPANY.text()
        String system,     // MonitoringContext.SYSTEM.text()
        String domain,     // MonitoringContext.DOMAIN.text()
        String message     // 에러/상태 메시지(문자열)
) {
}
