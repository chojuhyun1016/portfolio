package org.example.order.api.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * 표준 ContentCaching*Wrapper 기반 Request/Response 로깅
 * - include/exclude 패턴, 최대 바디 길이 설정 지원
 */
@Slf4j
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final Set<String> includePatterns;
    private final Set<String> excludePatterns;
    private final int maxPayloadLength;
    private final AntPathMatcher pm = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 제외 우선
        for (String p : excludePatterns) {
            if (pm.match(p, path)) {
                return true;
            }
        }

        // 포함 매치 없으면 스킵
        for (String p : includePatterns) {
            if (pm.match(p, path)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            logRequest(req);
            logResponse(res, elapsed);
            res.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper req) {
        StringBuilder headers = new StringBuilder();
        Enumeration<String> names = req.getHeaderNames();

        for (String name : Collections.list(names)) {
            headers.append(name).append('=').append(req.getHeader(name)).append("; ");
        }

        Charset cs = resolveCharset(req.getCharacterEncoding());
        String raw = new String(req.getContentAsByteArray(), cs);
        String body = truncate(raw, maxPayloadLength);

        log.info("---- REQUEST -----------------------------------------------------");
        log.info("method={} uri={} query={}", req.getMethod(), req.getRequestURI(), req.getQueryString());
        log.info("remote={}", req.getRemoteAddr());
        log.info("headers={}", headers);
        log.info("body={}", body);
    }

    private void logResponse(ContentCachingResponseWrapper res, long elapsed) {
        Charset cs = resolveCharset(res.getCharacterEncoding());
        String raw = new String(res.getContentAsByteArray(), cs);
        String body = truncate(raw, maxPayloadLength);
        HttpStatus status = HttpStatus.valueOf(res.getStatus());

        log.info("---- RESPONSE ----------------------------------------------------");
        log.info("status={} elapsedMs={}", status.value(), elapsed);
        log.info("body={}", body);
    }

    private static Charset resolveCharset(String enc) {
        if (enc == null || enc.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(enc);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }

        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
