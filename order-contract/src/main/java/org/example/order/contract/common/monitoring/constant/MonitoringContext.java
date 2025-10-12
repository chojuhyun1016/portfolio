package org.example.order.contract.common.monitoring.constant;

/**
 * 모니터링 메시지에 들어가는 고정 식별자(회사/시스템/도메인)
 * - 계약(와이어) 관점에서 "텍스트 값"만 중요
 */
public enum MonitoringContext {
    COMPANY("PORTFOLIO"),
    SYSTEM("EXAMPLE"),
    DOMAIN("ORDER");

    private final String text;

    MonitoringContext(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
