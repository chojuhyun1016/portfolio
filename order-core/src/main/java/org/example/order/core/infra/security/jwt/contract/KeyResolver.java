package org.example.order.core.infra.security.jwt.contract;

import java.security.Key;

/**
 * JWT Key 리졸버 인터페이스 (HMAC/RSA 등 지원)
 * - 키 해석 및 kid 제공
 */
public interface KeyResolver {
    Key resolveKey(String token);
    String getCurrentKid();
}
