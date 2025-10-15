package org.example.order.contract.shared.monitoring.type;

/**
 * 모니터링 유형(계약용)
 */
public enum MonitoringType {
    NORMAL(1), ERROR(2);

    private final int code;

    MonitoringType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
