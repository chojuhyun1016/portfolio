package org.example.order.core.infra.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.constant.JwtTokenConstants;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.example.order.core.infra.security.jwt.provider.RefreshTokenStore;
import org.example.order.core.infra.security.jwt.util.JwtHeaderResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 */
@Slf4j
@RequiredArgsConstructor
public class JwtSecurityAuthenticationFilter extends OncePerRequestFilter {

    protected static final String LOG_PREFIX = "[JwtFilter]";

    private final JwtTokenManager jwtTokenManager;
    private final RefreshTokenStore refreshTokenStore;

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

    protected void handleAuthentication(String token, HttpServletRequest request,
                                        HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        if (!jwtTokenManager.validateToken(token)) {
            handleUnauthorized(response, JwtTokenConstants.INVALID_TOKEN);

            return;
        }

        String jti = jwtTokenManager.getJti(token);
        if (!refreshTokenStore.validateRefreshToken(jti, token)) {  // jti → userId 도 가능
            handleUnauthorized(response, JwtTokenConstants.INVALID_JTI);

            return;
        }

        if (refreshTokenStore.isBlacklisted(token)) {
            handleUnauthorized(response, JwtTokenConstants.BLACKLISTED_TOKEN);

            return;
        }

        setSecurityContext(token, request);
        filterChain.doFilter(request, response);
    }

    protected void setSecurityContext(String token, HttpServletRequest request) {
        String userId = jwtTokenManager.getUserId(token);
        var authorities = jwtTokenManager.getRoles(token);  // ✅ 이제 SimpleGrantedAuthority 리스트

        var userDetails = new User(userId, "", authorities);  // ✅ 타입 문제 해결
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);  // ✅ 문제 해결
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("{} Authentication success - userId={}, authorities={}", LOG_PREFIX, userId, authorities);
    }

    protected void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        log.warn("{} {}", LOG_PREFIX, message);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}
