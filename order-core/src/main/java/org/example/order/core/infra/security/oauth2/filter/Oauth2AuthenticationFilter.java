package org.example.order.core.infra.security.oauth2.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.oauth2.core.contract.Oauth2TokenProvider;
import org.example.order.core.infra.security.oauth2.util.Oauth2HeaderResolver;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AccessToken 검증용 Spring Security 필터 (블랙리스트 + SecurityContext 등록)
 */
@Component
@RequiredArgsConstructor
public class Oauth2AuthenticationFilter extends OncePerRequestFilter {

    private final Oauth2TokenProvider tokenProvider;
    private final JwtTokenManager jwtTokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 요청 헤더에서 토큰 추출
        String token = Oauth2HeaderResolver.resolveToken(request);

        if (token != null) {
            // 1. 토큰 유효성 검증 (JWT + 블랙리스트 확인)
            boolean isValid = tokenProvider.validateAccessToken(token);

            if (!isValid) {
                // 인증 실패 시 401 반환 및 메시지 출력
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("AccessToken is invalid or blacklisted.");

                return;
            }

            // 2. JWT에서 사용자 ID 및 권한 정보 추출
            String userId = jwtTokenManager.getUserId(token);
            var authorities = jwtTokenManager.getRoles(token);

            // 3. 인증 객체 생성 및 SecurityContext에 등록
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
}
