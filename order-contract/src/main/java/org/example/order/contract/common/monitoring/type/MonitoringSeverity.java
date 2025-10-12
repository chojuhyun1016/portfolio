package org.example.order.contract.common.monitoring.type;

/**
 * 장애 심각도(계약용): 1~5
 * - 내부 의미 부여는 발신측/수신측 정책에 따름
 */
public enum MonitoringSeverity {
    LEVEL_1(1, "TRIVIAL"),
    LEVEL_2(2, "MINOR"),
    LEVEL_3(3, "MODERATE"),
    LEVEL_4(4, "MAJOR"),
    LEVEL_5(5, "CRITICAL");

    private final int level;
    private final String text;

    MonitoringSeverity(int level, String text) {
        this.level = level;
        this.text = text;
    }

    public int level() {
        return level;
    }

    public String text() {
        return text;
    }

    public static MonitoringSeverity ofLevel(int level) {
        for (var s : values()) if (s.level == level) return s;
        return null;
    }
}
