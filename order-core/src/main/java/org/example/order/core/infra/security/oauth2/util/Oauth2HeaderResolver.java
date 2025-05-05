package org.example.order.core.infra.security.oauth2.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Authorization 헤더에서 Bearer 토큰 추출 유틸.
 */
public class Oauth2HeaderResolver {

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출.
     * @param request HTTP 요청
     * @return 추출된 토큰 (없으면 null)
     */
    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // "Bearer " 접두사 제거 후 토큰 반환
        }

        return null;
    }
}
