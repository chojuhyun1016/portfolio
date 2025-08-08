package org.example.order.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 내부 전용 헤더 검증 필터.
 * - 설정(yml) 읽지 않음: headerName/secret/whitelist는 생성자 주입
 */
public class GatewayOnlyFilter extends OncePerRequestFilter {

    private final String headerName;
    private final String expectedSecret;
    private final Set<String> whiteListPatterns;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewayOnlyFilter(String headerName, String expectedSecret, Set<String> whiteListPatterns) {
        this.headerName = headerName;
        this.expectedSecret = expectedSecret;
        this.whiteListPatterns = whiteListPatterns;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        for (String p : whiteListPatterns) {
            if (pathMatcher.match(p, path)) {
                chain.doFilter(req, res);
                return;
            }
        }

        String actual = req.getHeader(headerName);
        if (!StringUtils.hasText(expectedSecret) || !StringUtils.hasText(actual) || !actual.equals(expectedSecret)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Gateway header missing or invalid\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}
