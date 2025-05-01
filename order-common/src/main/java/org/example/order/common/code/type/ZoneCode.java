package org.example.order.common.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZoneId;
import java.util.Arrays;

/**
 * 시스템에서 사용하는 주요 시간대 코드 정의
 *
 * - 각 enum 상수는 실제 ZoneId 문자열과 연결됨
 * - {@link #getZoneId()} 를 통해 java.time.ZoneId 객체 반환
 * - {@link #of(String)} 메서드를 통해 문자열로부터 안전하게 ZoneCode 매핑 가능
 */
@Getter
@RequiredArgsConstructor
public enum ZoneCode implements CodeEnum {

    UTC("UTC"),
    KR("Asia/Seoul"),
    US("America/Los_Angeles"),
    CA("America/Toronto"),
    TW("Asia/Taipei"),
    HK("Asia/Hong_Kong"),
    MY("Asia/Kuala_Lumpur"),
    SG("Asia/Singapore"),
    MX("America/Mexico_City");

    private final String text;

    /**
     * ZoneId 객체 반환
     *
     * @return ZoneId.of(text)
     */
    public ZoneId getZoneId() {
        return ZoneId.of(text);
    }

    /**
     * 문자열을 ZoneCode enum으로 변환 (대소문자 무시)
     *
     * @param zoneString "KR", "pt", "Us" 등
     * @return 일치하는 ZoneCode, 없으면 null
     */
    public static ZoneCode of(String zoneString) {
        if (zoneString == null || zoneString.isBlank()) {
            return null;
        }

        return Arrays.stream(ZoneCode.values())
                .filter(z -> z.name().equalsIgnoreCase(zoneString))
                .findFirst()
                .orElse(null);
    }
}
