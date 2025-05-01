package org.example.order.common.context;

import lombok.Builder;

@Builder
public record AccessUserInfo (
        Long userId,
        String loginId,
        String userType,
        String roles,
        String groups
) {
    public static AccessUserInfo system() {
        return AccessUserInfo.builder()
                .userId(0L)
                .loginId("")
                .userType("SYSTEM")
                .roles("")
                .groups("")
                .build();
    }

    public static AccessUserInfo unknown() {
        return AccessUserInfo.builder()
                .userId(0L)
                .loginId("")
                .userType("UNKNOWN")
                .roles("")
                .groups("")
                .build();
    }
}
