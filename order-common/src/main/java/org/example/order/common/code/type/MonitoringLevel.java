package org.example.order.common.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MonitoringLevel implements CodeEnum {
    DANGER("DANGER", 1),
    WARN("WARN", 2);

    private final String text;
    private final Integer level;
}
