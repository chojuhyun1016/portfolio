package org.example.order.common.core.monitoring.message.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;

import java.util.Arrays;
import java.util.Objects;

/**
 * 시스템 고정 모니터링 코드 Enum
 * - COMPANY, SYSTEM, DOMAIN 구분을 고정된 단일 Enum으로 표현
 * - 각 항목은 하나의 의미 있는 텍스트 값을 갖는다.
 */
@Getter
@RequiredArgsConstructor
public enum MonitoringContextCode implements CodeEnum {

    // 회사 식별자
    COMPANY("PORTFOLIO"),

    // 시스템 식별자
    SYSTEM("EXAMPLE"),

    // 도메인 식별자
    DOMAIN("ORDER");

    // 실제 텍스트 값 (예: 로그, 알림, 외부 시스템 연동에 사용될 값)
    private final String text;

    /**
     * 문자열을 enum으로 변환
     * 대소문자 무시하며, null 입력 시 null 반환
     *
     * @param code 문자열 코드 (예: "COMPANY")
     * @return 해당하는 MonitoringCode enum 또는 null
     */
    public static MonitoringContextCode of(String code) {
        if (Objects.isNull(code)) {
            return null;
        }

        return Arrays.stream(MonitoringContextCode.values())
                .filter(item -> item.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
