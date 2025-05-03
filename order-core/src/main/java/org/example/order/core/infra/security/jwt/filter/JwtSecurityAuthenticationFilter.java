package org.example.order.core.infra.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.auth.store.AuthTokenStoreService;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * - 모든 요청마다 실행되며, JWT 토큰을 검증합니다.
 * - 유효한 토큰이 있으면 Spring Security Context에 인증 객체를 세팅합니다.
 * - 블랙리스트된 토큰 또는 재사용된 JTI 토큰은 차단합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtSecurityAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;
    private final AuthTokenStoreService authTokenStoreService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 요청 헤더에서 토큰 추출
        String token = jwtTokenManager.resolveToken(request);

        if (token != null) {
            // 토큰이 존재할 경우 검증
            if (jwtTokenManager.validateToken(token)) {
                String jti = jwtTokenManager.getJti(token);

                // JTI (토큰 고유 ID) 검증
                if (!authTokenStoreService.isJtiValid(jti)) {
                    log.warn("[JWT] JTI invalid or reused - jti={}", jti);
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JTI (reused or expired)");
                    return;
                }

                // 블랙리스트 토큰 검증
                if (authTokenStoreService.isBlacklisted(token)) {
                    log.warn("[JWT] Token is blacklisted - token={}", token);
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted");
                    return;
                }

                // 정상 토큰: 인증 객체 생성 및 SecurityContext 설정
                String userId = jwtTokenManager.getUserId(token);
                var authorities = jwtTokenManager.getRoles(token);

                var userDetails = new User(userId, "", authorities);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("[JWT] Authentication success - userId={}, authorities={}", userId, authorities);

            } else {
                // 유효하지 않은 토큰
                log.warn("[JWT] Invalid or expired token");
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        } else {
            // 토큰이 없는 경우
            log.debug("[JWT] No token found in request");
        }

        // 다음 필터로 계속 진행
        filterChain.doFilter(request, response);
    }
}
