package org.example.order.common.event;

import org.example.order.common.code.MonitoringType;

public record MonitoringMessage(
        Integer type,
        String companyName,
        String systemName,
        String domainName,
        String exceptionMessage,
        Integer level
) {
    public static MonitoringMessage toMessage(MonitoringType type, DlqMessage message) {
        return new MonitoringMessage(type.getLevel(), "PORTFOLIO", "EXAMPLE", "ORDER", "[테스트 발송]" + message.getError().getMsg(), message.getError().getLevelCode());
    }
}
