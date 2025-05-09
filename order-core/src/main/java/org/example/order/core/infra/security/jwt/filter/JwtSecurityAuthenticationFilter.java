// ✅ JwtSecurityAuthenticationFilter.java
package org.example.order.core.infra.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.example.order.core.infra.security.jwt.util.JwtHeaderResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 (RefreshToken 검증은 외부 서비스에서 처리)
 */
@Slf4j
@RequiredArgsConstructor
public class JwtSecurityAuthenticationFilter extends OncePerRequestFilter {

    protected static final String LOG_PREFIX = "[JwtFilter]";

    private final JwtTokenManager jwtTokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = JwtHeaderResolver.resolveToken(request);

        if (token != null) {
            // 이제는 단순 AccessToken 검증만 수행
            if (jwtTokenManager.validateToken(token)) {
                setSecurityContext(token, request);
            } else {
                log.warn("{} Invalid token detected", LOG_PREFIX);
                SecurityContextHolder.clearContext();
            }
        } else {
            log.debug("{} No token found in request", LOG_PREFIX);
        }

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
}
