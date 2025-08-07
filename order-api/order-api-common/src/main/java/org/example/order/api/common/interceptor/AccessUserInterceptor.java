package org.example.order.api.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.order.common.core.context.AccessUserContext;
import org.example.order.common.core.context.AccessUserInfo;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * 요청 헤더에서 사용자 정보를 추출해 AccessUserContext에 저장하는 인터셉터
 */
public class AccessUserInterceptor implements HandlerInterceptor {

    private static final String HEADER_USER_ID = "x-user-id";
    private static final String HEADER_LOGIN_ID = "x-login-id";
    private static final String HEADER_USER_TYPE = "x-user-type";
    private static final String HEADER_ROLES = "x-roles";
    private static final String HEADER_GROUPS = "x-groups";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var userId = parseLong(request.getHeader(HEADER_USER_ID));
        var loginId = defaultString(request.getHeader(HEADER_LOGIN_ID));
        var userType = defaultString(request.getHeader(HEADER_USER_TYPE), "UNKNOWN");
        var roles = defaultString(request.getHeader(HEADER_ROLES));
        var groups = defaultString(request.getHeader(HEADER_GROUPS));

        AccessUserContext.setAccessUser(AccessUserInfo.builder()
                .userId(userId)
                .loginId(loginId)
                .userType(userType)
                .roles(roles)
                .groups(groups)
                .build());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AccessUserContext.clear();
    }

    private Long parseLong(String value) {
        try {
            return Objects.nonNull(value) ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String defaultString(String value) {
        return value != null ? value : "";
    }

    private String defaultString(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
