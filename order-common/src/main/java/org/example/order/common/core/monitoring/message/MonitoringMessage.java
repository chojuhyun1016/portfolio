package org.example.order.common.core.monitoring.message;

import org.example.order.common.core.monitoring.message.code.MonitoringContextCode;
import org.example.order.common.core.monitoring.message.code.MonitoringLevelCode;
import org.example.order.common.core.monitoring.message.code.MonitoringType;
import org.example.order.common.core.messaging.message.DlqMessage;

import java.util.Optional;

/**
 * Kafka로 전송되는 모니터링 메시지 구조
 * - 장애 발생 시 DLQ 처리 대상의 메타데이터 및 예외 메시지를 포함한다.
 */
public record MonitoringMessage(
        Integer type,             // 모니터링 유형: NORMAL, ERROR 등 (MonitoringType.getLevel())
        Integer level,            // 장애 심각도: 1~5 (MonitoringLevelCode.getLevel())
        String companyName,       // 고정 회사 식별자 (예: "PORTFOLIO")
        String systemName,        // 고정 시스템 식별자 (예: "EXAMPLE")
        String domainName,        // 고정 도메인 식별자 (예: "ORDER")
        String exceptionMessage   // DLQ에 포함된 예외 메시지 (Throwable.getMessage())
) {
    /**
     * MonitoringMessage 팩토리 메서드
     * - DLQ 메시지와 함께 모니터링 유형 및 레벨을 조합하여 메시지를 생성한다.
     *
     * @param type    모니터링 유형 (예: MonitoringType.ERROR)
     * @param level   장애 심각도 코드 (예: MonitoringLevelCode.CRITICAL)
     * @param message DLQ 메시지 객체 (예외 포함)
     * @return 구성된 모니터링 메시지
     */
    public static MonitoringMessage toMessage(MonitoringType type, MonitoringLevelCode level, DlqMessage message) {
        return new MonitoringMessage(
                type.getLevel(),                                   // 모니터링 유형 코드
                level.getLevel(),                                  // 장애 심각도 정수값 (1~5)
                MonitoringContextCode.COMPANY.getText(),           // 고정 회사명
                MonitoringContextCode.SYSTEM.getText(),            // 고정 시스템명
                MonitoringContextCode.DOMAIN.getText(),            // 고정 도메인명
                Optional.ofNullable(message.getError())            // 예외 메시지 추출
                        .map(Throwable::getMessage)
                        .orElse("unknown")
        );
    }
}
