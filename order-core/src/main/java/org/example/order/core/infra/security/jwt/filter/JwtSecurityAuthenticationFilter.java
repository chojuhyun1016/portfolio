package org.example.order.core.infra.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.constant.JwtErrorConstants;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.example.order.core.infra.security.jwt.store.RefreshTokenStore;
import org.example.order.core.infra.security.jwt.util.JwtHeaderResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 - AccessToken 검증 & SecurityContext 설정
 */
@Slf4j
@RequiredArgsConstructor
public class JwtSecurityAuthenticationFilter extends OncePerRequestFilter {

    protected static final String LOG_PREFIX = "[JwtFilter]";

    private final JwtTokenManager jwtTokenManager;
    private final RefreshTokenStore refreshTokenStore;

    /**
     * 필터 실행 - JWT 토큰 파싱 및 인증 처리
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = JwtHeaderResolver.resolveToken(request);

        if (token != null) {
            handleAuthentication(token, request, response, filterChain);
        } else {
            log.debug("{} No token found in request", LOG_PREFIX);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 토큰 검증 및 SecurityContext 설정
     */
    protected void handleAuthentication(String token,
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws IOException, ServletException {

        // 1. 토큰 유효성 검사
        if (!jwtTokenManager.validateToken(token)) {
            handleUnauthorized(response, JwtErrorConstants.INVALID_TOKEN);

            return;
        }

        // 2. Refresh 토큰 유효성 검사 (DB/Redis)
        String jti = jwtTokenManager.getJti(token);
        if (!refreshTokenStore.validateRefreshToken(jti, token)) {
            handleUnauthorized(response, JwtErrorConstants.INVALID_JTI);

            return;
        }

        // 3. 블랙리스트 여부 검사
        if (refreshTokenStore.isBlacklisted(token)) {
            handleUnauthorized(response, JwtErrorConstants.BLACKLISTED_TOKEN);

            return;
        }

        // 4. SecurityContext 설정 후 다음 필터 진행
        setSecurityContext(token, request);
        filterChain.doFilter(request, response);
    }

    /**
     * SecurityContextHolder에 인증 정보 설정
     */
    protected void setSecurityContext(String token, HttpServletRequest request) {
        String userId = jwtTokenManager.getUserId(token);
        var authorities = jwtTokenManager.getRoles(token);

        var userDetails = new User(userId, "", authorities);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("{} Authentication success - userId={}, authorities={}", LOG_PREFIX, userId, authorities);
    }

    /**
     * 인증 실패 처리 - 401 에러 반환
     */
    protected void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        log.warn("{} {}", LOG_PREFIX, message);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}
