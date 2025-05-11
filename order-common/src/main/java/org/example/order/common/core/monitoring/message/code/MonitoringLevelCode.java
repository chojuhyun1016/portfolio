package org.example.order.common.core.monitoring.message.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;

import java.util.Arrays;
import java.util.Objects;

/**
 * 장애 심각도 레벨 코드
 * - 1 (낮음) ~ 5 (치명적) 정수 기반 레벨
 */
@Getter
@RequiredArgsConstructor
public enum MonitoringLevelCode implements CodeEnum {

    LEVEL_1("TRIVIAL", 1, "정보성 또는 경고 없음"),
    LEVEL_2("MINOR", 2, "경미한 경고"),
    LEVEL_3("MODERATE", 3, "주의가 필요한 상태"),
    LEVEL_4("MAJOR", 4, "심각한 오류"),
    LEVEL_5("CRITICAL", 5, "치명적 시스템 장애");

    private final String text;         // 영문 식별자 (TRIVIAL, MINOR 등)
    private final int level;           // 수치화된 심각도 (1~5)
    private final String description;  // 한글 설명

    /**
     * 정수 레벨 값으로 enum 찾기
     */
    public static MonitoringLevelCode of(Integer level) {
        if (Objects.isNull(level)) {
            return null;
        }

        return Arrays.stream(values())
                .filter(v -> v.level == level)
                .findFirst()
                .orElse(null);
    }

    /**
     * 텍스트 값(TRIVIAL 등)으로 enum 찾기
     */
    public static MonitoringLevelCode fromText(String text) {
        if (text == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(v -> v.text.equalsIgnoreCase(text))
                .findFirst()
                .orElse(null);
    }
}
