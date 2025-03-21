package org.example.order.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MonitoringType implements CodeEnum {
    NORMAL("일반", 1),
    ERROR("장애", 2);

    private final String text;
    private final Integer level;
}
