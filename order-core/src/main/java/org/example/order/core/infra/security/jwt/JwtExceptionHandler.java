package org.example.order.core.infra.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

@Slf4j
@ControllerAdvice
public class JwtExceptionHandler {

    @ExceptionHandler(ExpiredJwtException.class)
    public void handleExpiredToken(ExpiredJwtException e, HttpServletResponse response) throws IOException {
        log.warn("Expired JWT token: {}", e.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
    }

    @ExceptionHandler(AuthenticationException.class)
    public void handleAuthException(AuthenticationException e, HttpServletResponse response) throws IOException {
        log.warn("Authentication error: {}", e.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
    }
}
