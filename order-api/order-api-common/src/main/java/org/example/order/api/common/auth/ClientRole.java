package org.example.order.api.common.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;

/**
 * API 클라이언트 인증 권한 역할
 * - Spring Security 또는 인증 처리 시 사용됨
 */
@Getter
@RequiredArgsConstructor
public enum ClientRole implements CodeEnum {

    ROLE_CLIENT("ROLE_CLIENT");

    private final String text;
}
