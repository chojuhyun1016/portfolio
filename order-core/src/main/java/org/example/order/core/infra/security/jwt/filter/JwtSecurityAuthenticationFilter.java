package org.example.order.core.infra.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.auth.service.AuthTokenStoreService;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtSecurityAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenManager tokenProvider;
    private final AuthTokenStoreService authTokenStoreService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = tokenProvider.resolveToken(request);

        if (token != null && tokenProvider.validateToken(token)) {
            String jti = tokenProvider.getJti(token);

            if (!authTokenStoreService.isJtiValid(jti)) {
                log.warn("[JWT] JTI invalid or reused");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token (reused or expired)");
                return;
            }

            if (authTokenStoreService.isBlacklisted(token)) {
                log.warn("[JWT] Token is blacklisted");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token blacklisted");
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    tokenProvider.getUserId(token),
                    null,
                    tokenProvider.getRoles(token)
            );

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
