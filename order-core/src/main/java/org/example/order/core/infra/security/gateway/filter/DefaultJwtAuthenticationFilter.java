package org.example.order.core.infra.security.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.gateway.matcher.WhiteListMatcher;
import org.example.order.core.infra.security.gateway.validator.JwtTokenValidator;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 기본 JWT 인증 필터 구현 (Global Filter)
 */
@Slf4j
@Component
public class DefaultJwtAuthenticationFilter extends AbstractJwtAuthenticationFilter implements Ordered {

    public DefaultJwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator, WhiteListMatcher whiteListMatcher) {
        super(jwtTokenValidator, whiteListMatcher);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
