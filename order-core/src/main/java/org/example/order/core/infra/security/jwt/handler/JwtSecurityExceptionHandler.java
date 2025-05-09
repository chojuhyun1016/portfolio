package org.example.order.core.infra.security.jwt.handler;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

/**
 * JWT 인증 관련 글로벌 예외 처리기
 * - JWT가 만료되었거나 인증 실패 시 일관된 에러 응답을 제공
 */
@Slf4j
@ControllerAdvice
public class JwtSecurityExceptionHandler {

    /**
     * JWT 만료 예외 처리
     * - 토큰이 만료되었을 때 401 UNAUTHORIZED 응답 반환
     *
     * @param e        ExpiredJwtException 인스턴스
     * @param response HttpServletResponse
     * @throws IOException 예외 처리 중 IO 발생 시
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public void handleExpiredToken(ExpiredJwtException e, HttpServletResponse response) throws IOException {
        log.warn("Expired JWT token: {}", e.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
    }

    /**
     * 스프링 시큐리티 인증 예외 처리
     * - 인증 실패(예: 잘못된 토큰, 권한 없음 등) 시 401 UNAUTHORIZED 반환
     *
     * @param e        AuthenticationException 인스턴스
     * @param response HttpServletResponse
     * @throws IOException 예외 처리 중 IO 발생 시
     */
    @ExceptionHandler(AuthenticationException.class)
    public void handleAuthException(AuthenticationException e, HttpServletResponse response) throws IOException {
        log.warn("🔒 Authentication error: {}", e.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
    }
}
