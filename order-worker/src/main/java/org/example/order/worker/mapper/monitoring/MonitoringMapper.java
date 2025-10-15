package org.example.order.worker.mapper.monitoring;

import org.example.order.contract.shared.monitoring.ctx.MonitoringContext;
import org.example.order.contract.shared.monitoring.msg.MonitoringMessage;
import org.example.order.contract.shared.monitoring.type.MonitoringSeverity;
import org.example.order.contract.shared.monitoring.type.MonitoringType;
import org.springframework.stereotype.Component;

/**
 * 내부 예외/상태 -> 계약 메시지 변환
 * - 내부 타입 의존(예: CustomErrorMessage, DlqMessage 등)은 여기서만 처리
 * - 계약에는 "문자열/숫자"만 넣어서 느슨하게 보낸다
 */
@Component
public class MonitoringMapper {

    public MonitoringMessage error(String errorText, MonitoringSeverity severity) {
        return new MonitoringMessage(
                MonitoringType.ERROR.code(),
                severity.level(),
                MonitoringContext.COMPANY.text(),
                MonitoringContext.SYSTEM.text(),
                MonitoringContext.DOMAIN.text(),
                (errorText == null || errorText.isBlank()) ? "unknown" : errorText
        );
    }

    public MonitoringMessage normal(String msg) {
        return new MonitoringMessage(
                MonitoringType.NORMAL.code(),
                MonitoringSeverity.LEVEL_1.level(),
                MonitoringContext.COMPANY.text(),
                MonitoringContext.SYSTEM.text(),
                MonitoringContext.DOMAIN.text(),
                (msg == null || msg.isBlank()) ? "ok" : msg
        );
    }
}
