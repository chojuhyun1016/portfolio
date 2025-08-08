package org.example.order.common.core.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;

/**
 * 호출자 컨텍스트 표준 모델 (전 모듈 공용)
 * - 게이트웨이/헤더 기반 값 바인딩
 * - roles, groups는 CSV 문자열로 보존하되, 편의 List getter 제공
 */
@Builder
public record AccessUserInfo(
        Long userId,    // X-User-Id (숫자 파싱 실패 시 0L)
        String loginId,   // X-Login-Id (없으면 "")
        String userType,  // X-User-Type (SYSTEM/USER/PARTNER/UNKNOWN...)
        String roles,     // X-Client-Roles (CSV: "ROLE_A,ROLE_B")
        String groups     // X-Client-Groups (CSV)
) {
    public static AccessUserInfo system() {
        return AccessUserInfo.builder()
                .userId(0L).loginId("").userType("SYSTEM")
                .roles("").groups("")
                .build();
    }

    public static AccessUserInfo unknown() {
        return AccessUserInfo.builder()
                .userId(0L).loginId("").userType("UNKNOWN")
                .roles("").groups("")
                .build();
    }

    /**
     * roles CSV → List
     */
    public List<String> roleList() {
        return csvToList(roles);
    }

    /**
     * groups CSV → List
     */
    public List<String> groupList() {
        return csvToList(groups);
    }

    private static List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
