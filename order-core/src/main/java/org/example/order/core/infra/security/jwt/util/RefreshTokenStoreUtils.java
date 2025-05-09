package org.example.order.core.infra.security.jwt.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * RefreshTokenStore 공용 유틸 클래스
 * - 만료 판단, TTL 계산 등 구현체에서 공통으로 사용할 기능 제공.
 */
public final class RefreshTokenStoreUtils {

    // 인스턴스화 방지
    private RefreshTokenStoreUtils() {}

    /**
     * 주어진 만료 시간이 현재보다 이전인지 여부를 판단.
     *
     * @param expiry 만료 일시
     * @return true = 만료됨 / false = 아직 유효
     */
    public static boolean isExpired(LocalDateTime expiry) {
        return expiry.isBefore(LocalDateTime.now());
    }

    /**
     * 현재 시각으로부터 만료 시각까지 남은 TTL(초)을 반환.
     * (만약 이미 만료되었으면 -1 반환)
     *
     * @param expiry 만료 일시
     * @return 남은 TTL (초)
     */
    public static long calcTtlSeconds(LocalDateTime expiry) {
        long seconds = Duration.between(LocalDateTime.now(), expiry).getSeconds();

        return seconds > 0 ? seconds : -1L;
    }

    /**
     * 간단한 null 체크 유틸 (필요 시 사용)
     *
     * @param obj     검사할 객체
     * @param message 예외 메시지
     * @param <T>     타입
     * @return obj 그대로 반환
     * @throws IllegalArgumentException null일 경우 예외
     */
    public static <T> T ensureNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }
}
